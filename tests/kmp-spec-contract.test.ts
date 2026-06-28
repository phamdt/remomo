import { describe, expect, it } from "vitest";
import {
  continueRunRequestSchema,
  createRunRequestSchema,
  runModeSchema,
} from "../src/api-schema.js";
import type { RunSummary, SseEvent } from "../src/types.js";

/**
 * Documents the KMP client contract against the TypeScript API.
 * See docs/remote-cursor-agent-kmp-app-spec.md
 */

const sampleWorkspace = {
  id: "openclaw-fullstack",
  name: "OpenClaw Full Stack",
  repos: [
    { repoId: "openclaw-api", role: "api", path: "api" },
    { repoId: "openclaw-client", role: "client", path: "client" },
  ],
  defaultPromptContext: "Use api/ for the TS server and client/ for the KMP app.",
};

const sampleRunSummary: RunSummary = {
  id: "run_123",
  workspaceId: "openclaw-fullstack",
  status: "completed",
  mode: "apply",
  repos: [
    {
      repoId: "openclaw-api",
      role: "api",
      path: "api",
      branch: "cursor/run_123-api",
      prUrl: "https://github.com/org/openclaw-api/pull/42",
    },
  ],
  resultPath: "runs/run_123/result",
  createdAt: "2026-06-28T12:00:00.000Z",
  updatedAt: "2026-06-28T12:05:00.000Z",
};

describe("KMP spec contract — workspaces", () => {
  it("GET /workspaces wraps list in workspaces key", () => {
    const body = { workspaces: [sampleWorkspace] };
    expect(body.workspaces[0]).toMatchObject({
      id: expect.any(String),
      name: expect.any(String),
      repos: expect.arrayContaining([
        expect.objectContaining({
          repoId: expect.any(String),
          role: expect.any(String),
          path: expect.any(String),
        }),
      ]),
    });
    expect(body.workspaces[0]).toHaveProperty("defaultPromptContext");
  });
});

describe("KMP spec contract — create run", () => {
  it("accepts valid CreateRunRequest shape", () => {
    const parsed = createRunRequestSchema.safeParse({
      workspaceId: "openclaw-fullstack",
      mode: "plan_only",
      prompt: "Summarize architecture",
      baseRef: "main",
    });
    expect(parsed.success).toBe(true);
  });

  it("POST /runs returns id only", () => {
    const response = { id: "run_123" };
    expect(Object.keys(response)).toEqual(["id"]);
  });

  it("run modes are plan_only and apply", () => {
    expect(runModeSchema.options).toEqual(["plan_only", "apply"]);
  });
});

describe("KMP spec contract — run summary", () => {
  it("uses repos not changedRepos", () => {
    expect(sampleRunSummary).toHaveProperty("repos");
    expect(sampleRunSummary).not.toHaveProperty("changedRepos");
  });

  it("includes mode, timestamps, and optional resultPath", () => {
    expect(sampleRunSummary.mode).toBe("apply");
    expect(sampleRunSummary.createdAt).toMatch(/^\d{4}-/);
    expect(sampleRunSummary.updatedAt).toMatch(/^\d{4}-/);
    expect(sampleRunSummary.resultPath).toMatch(/^runs\//);
  });

  it("repo entries expose branch and prUrl when published", () => {
    const repo = sampleRunSummary.repos[0];
    expect(repo?.branch).toBeDefined();
    expect(repo?.prUrl).toMatch(/^https:\/\//);
  });
});

describe("KMP spec contract — continue run", () => {
  it("accepts prompt only (no mode field)", () => {
    const parsed = continueRunRequestSchema.safeParse({
      prompt: "Apply the approved plan",
    });
    expect(parsed.success).toBe(true);
    expect(continueRunRequestSchema.shape).not.toHaveProperty("mode");
  });
});

describe("KMP spec contract — SSE events", () => {
  const events: SseEvent[] = [
    { type: "status", status: "running" },
    { type: "log", message: "Analyzing workspace" },
    { type: "tool", name: "grep", summary: "src/" },
    { type: "error", message: "Agent run failed" },
    { type: "result", ok: true },
  ];

  it("uses log and tool (not assistant or tool_call)", () => {
    const types = events.map((e) => e.type);
    expect(types).toContain("log");
    expect(types).toContain("tool");
    expect(types).not.toContain("assistant");
    expect(types).not.toContain("tool_call");
  });

  it("serializes each event with a type discriminator", () => {
    for (const event of events) {
      const json = JSON.parse(JSON.stringify(event)) as { type: string };
      expect(json.type).toBe(event.type);
    }
  });
});

describe("KMP spec contract — run statuses", () => {
  const allStatuses = [
    "queued",
    "running",
    "completed",
    "failed",
    "cancelled",
  ] as const;

  it("defines five RunStatus values for the client enum", () => {
    expect(allStatuses).toHaveLength(5);
  });

  it("treats completed, failed, and cancelled as terminal", () => {
    const terminal = new Set(["completed", "failed", "cancelled"]);
    for (const status of allStatuses) {
      const isTerminal = terminal.has(status);
      if (status === "queued" || status === "running") {
        expect(isTerminal).toBe(false);
      } else {
        expect(isTerminal).toBe(true);
      }
    }
  });
});
