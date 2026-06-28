import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AgentRunner } from "../src/agent/runner.js";
import type { AgentRunnerOptions, AgentRunnerResult } from "../src/agent/runner.js";
import { RunDatabase } from "../src/db/client.js";
import { RunAlreadyActiveError, RunNotActiveError } from "../src/errors.js";
import { safeEqualToken } from "../src/middleware/auth.js";
import { getDataRoot } from "../src/paths.js";
import { RunService } from "../src/services/run-service.js";
import type { AppSecrets } from "../src/security/secrets.js";

const secrets: AppSecrets = {
  remoteAgentToken: "read-token",
  cursorApiKey: "cursor-key",
  modelId: "test-model",
};

function mockRunner(
  impl: (options: AgentRunnerOptions) => Promise<AgentRunnerResult>,
): AgentRunner {
  return { run: impl } as AgentRunner;
}

describe("security audit regressions", () => {
  const tempDirs: string[] = [];

  afterEach(() => {
    vi.restoreAllMocks();
    for (const dir of tempDirs.splice(0)) {
      fs.rmSync(dir, { recursive: true, force: true });
    }
    delete process.env.REMOTE_AGENT_DATA;
  });

  function setupService(runner: AgentRunner) {
    const dataDir = fs.mkdtempSync(path.join(os.tmpdir(), "audit-"));
    tempDirs.push(dataDir);
    const configDir = path.join(dataDir, "config");
    fs.mkdirSync(configDir, { recursive: true });
    fs.writeFileSync(
      path.join(configDir, "repos.json"),
      JSON.stringify([
        {
          id: "demo-api",
          name: "Demo",
          url: "https://github.com/octocat/Hello-World.git",
          defaultBranch: "master",
          enabled: true,
        },
      ]),
    );
    fs.writeFileSync(
      path.join(configDir, "workspaces.json"),
      JSON.stringify([
        {
          id: "demo-workspace",
          name: "Demo",
          repos: [{ repoId: "demo-api", role: "api", path: "api" }],
        },
      ]),
    );
    process.env.REMOTE_AGENT_DATA = dataDir;

    const dbPath = path.join(dataDir, "data", "runs.sqlite");
    fs.mkdirSync(path.dirname(dbPath), { recursive: true });
    const db = new RunDatabase(dbPath);
    const service = new RunService({
      db,
      runner,
      secrets,
      configDir,
      repoCacheDir: path.join(dataDir, "repo-cache"),
      maxConcurrentRuns: 2,
      timeoutMs: 5_000,
    });
    return { service, db, dataDir };
  }

  it("token comparison rejects different lengths without timingSafeEqual on mismatched buffers", () => {
    expect(safeEqualToken("short", "much-longer-token")).toBe(false);
    expect(safeEqualToken("", "a")).toBe(false);
  });

  it("agent host env scrubbing temporarily removes REMOTE_AGENT_DATA from process.env", async () => {
    let dataRootDuringAgent = "";
    const runner = new AgentRunner();
    const runImpl = vi.spyOn(
      runner as unknown as { runWithSafeEnv: (o: AgentRunnerOptions) => Promise<AgentRunnerResult> },
      "runWithSafeEnv",
    );
    runImpl.mockImplementation(async () => {
      dataRootDuringAgent = getDataRoot();
      return { ok: true, resultText: "ok" };
    });

    const dataDir = fs.mkdtempSync(path.join(os.tmpdir(), "audit-env-"));
    tempDirs.push(dataDir);
    process.env.REMOTE_AGENT_DATA = dataDir;

    const signal = new AbortController().signal;
    await runner.run({
      runId: "run_test",
      workspaceRoot: dataDir,
      cursorStatePath: path.join(dataDir, "cursor-state"),
      mode: "plan_only",
      prompt: "work",
      eventsPath: path.join(dataDir, "events.jsonl"),
      modelId: "test",
      apiKey: "key",
      timeoutMs: 1_000,
      signal,
    });

    expect(dataRootDuringAgent).not.toBe(dataDir);
    expect(getDataRoot()).toBe(dataDir);
  });

  it("runs left in running status after simulated crash cannot continue or cancel", async () => {
    const { service, db } = setupService(
      mockRunner(async () => ({ ok: true, resultText: "ok" })),
    );

    vi.spyOn(
      service as unknown as { prepareWorktrees: () => Promise<void> },
      "prepareWorktrees",
    ).mockResolvedValue(undefined);

    const { id } = service.createRun(
      { workspaceId: "demo-workspace", mode: "plan_only", prompt: "work" },
      "read-token",
    );

    await new Promise((r) => setTimeout(r, 30));
    db.updateStatus(id, "running", new Date().toISOString());

    expect(() => service.continueRun(id, "again", "read-token")).toThrow(
      RunAlreadyActiveError,
    );
    expect(() => service.cancelRun(id)).toThrow(RunNotActiveError);
  });

  it("serializes agent execution through a single global runner queue", async () => {
    const order: string[] = [];
    const gates = new Map<string, () => void>();
    const runner = new AgentRunner();
    const runImpl = vi.spyOn(
      runner as unknown as { runWithSafeEnv: (o: AgentRunnerOptions) => Promise<AgentRunnerResult> },
      "runWithSafeEnv",
    );
    runImpl.mockImplementation(async (options) => {
      order.push(`start-${options.runId}`);
      await new Promise<void>((resolve) => {
        gates.set(options.runId, resolve);
      });
      order.push(`end-${options.runId}`);
      return { ok: true, resultText: "ok" };
    });

    const dataDir = fs.mkdtempSync(path.join(os.tmpdir(), "audit-queue-"));
    tempDirs.push(dataDir);
    const base = {
      workspaceRoot: dataDir,
      cursorStatePath: path.join(dataDir, "cursor-state"),
      mode: "plan_only" as const,
      eventsPath: path.join(dataDir, "events.jsonl"),
      modelId: "test",
      apiKey: "key",
      timeoutMs: 1_000,
      signal: new AbortController().signal,
    };

    const first = runner.run({ ...base, runId: "run_a", prompt: "one" });
    const second = runner.run({ ...base, runId: "run_b", prompt: "two" });

    await new Promise((r) => setTimeout(r, 20));
    expect(order.filter((e) => e.startsWith("start-"))).toHaveLength(1);

    gates.get("run_a")!();
    await new Promise((r) => setTimeout(r, 20));
    expect(order.filter((e) => e.startsWith("start-"))).toHaveLength(2);

    gates.get("run_b")!();
    await Promise.all([first, second]);
    expect(order).toEqual(["start-run_a", "end-run_a", "start-run_b", "end-run_b"]);
  });
});
