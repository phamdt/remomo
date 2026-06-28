import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { RunDatabase } from "../src/db/client.js";

describe("RunDatabase", () => {
  const tempDirs: string[] = [];

  afterEach(() => {
    for (const dir of tempDirs.splice(0)) {
      fs.rmSync(dir, { recursive: true, force: true });
    }
  });

  function tempDb(): RunDatabase {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), "runs-db-"));
    tempDirs.push(dir);
    return new RunDatabase(path.join(dir, "runs.sqlite"));
  }

  it("inserts and reads a run with repos", () => {
    const db = tempDb();
    const now = new Date().toISOString();
    db.insertRun(
      {
        id: "run_test",
        workspaceId: "demo",
        status: "queued",
        mode: "plan_only",
        runPath: "/tmp/run_test",
        cursorStatePath: "/tmp/run_test/cursor-state",
        createdAt: now,
        updatedAt: now,
      },
      [
        {
          repoId: "demo-api",
          role: "api",
          path: "api",
          baseRef: "main",
          branch: null,
          prUrl: null,
        },
      ],
    );

    const run = db.getRun("run_test");
    expect(run?.workspaceId).toBe("demo");
    expect(db.listRunRepos("run_test")).toHaveLength(1);
    db.close();
  });

  it("updates status and branch metadata", () => {
    const db = tempDb();
    const now = new Date().toISOString();
    db.insertRun(
      {
        id: "run_2",
        workspaceId: "demo",
        status: "queued",
        mode: "apply",
        runPath: "/tmp/run_2",
        cursorStatePath: "/tmp/run_2/cursor-state",
        createdAt: now,
        updatedAt: now,
      },
      [
        {
          repoId: "demo-api",
          role: "api",
          path: "api",
          baseRef: "main",
          branch: null,
          prUrl: null,
        },
      ],
    );

    db.updateStatus("run_2", "running", now);
    db.updateRunRepoBranch("run_2", "demo-api", "cursor/run_2-api", "https://example.com/pr/1");
    expect(db.getRun("run_2")?.status).toBe("running");
    expect(db.listRunRepos("run_2")[0]?.prUrl).toBe("https://example.com/pr/1");
    db.close();
  });

  it("lists runs newest first with optional workspace filter", () => {
    const db = tempDb();
    const older = "2026-01-01T00:00:00.000Z";
    const newer = "2026-01-02T00:00:00.000Z";
    const insert = (id: string, workspaceId: string, updatedAt: string) => {
      db.insertRun(
        {
          id,
          workspaceId,
          status: "completed",
          mode: "plan_only",
          runPath: `/tmp/${id}`,
          cursorStatePath: `/tmp/${id}/cursor-state`,
          createdAt: older,
          updatedAt,
        },
        [
          {
            repoId: "demo-api",
            role: "api",
            path: "api",
            baseRef: "main",
            branch: null,
            prUrl: null,
          },
        ],
      );
    };

    insert("run_old", "ws-a", older);
    insert("run_new", "ws-a", newer);
    insert("run_other", "ws-b", newer);

    expect(db.listRuns({ limit: 10 }).map((run) => run.id)).toEqual([
      "run_new",
      "run_other",
      "run_old",
    ]);
    expect(db.listRuns({ limit: 10, workspaceId: "ws-a" }).map((run) => run.id)).toEqual([
      "run_new",
      "run_old",
    ]);

    db.updateMode("run_old", "apply", newer);
    expect(db.getRun("run_old")?.mode).toBe("apply");
    db.close();
  });
});
