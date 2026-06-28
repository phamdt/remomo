import { describe, expect, it } from "vitest";
import type { Workspace } from "../src/config-schema.js";
import { InvalidRunOptionsError } from "../src/errors.js";
import { resolveBaseRef, validateRunOptions } from "../src/run-options.js";

const workspace: Workspace = {
  id: "demo",
  name: "Demo",
  repos: [{ repoId: "demo-api", role: "api", path: "api" }],
  runOptions: [
    {
      key: "baseRef",
      label: "Base branch",
      required: false,
      choices: [
        { value: "main", label: "main" },
        { value: "develop", label: "develop" },
      ],
    },
  ],
};

describe("run options", () => {
  it("resolves baseRef from options", () => {
    expect(
      resolveBaseRef(workspace, {
        workspaceId: "demo",
        mode: "plan_only",
        prompt: "hello",
        options: { baseRef: "develop" },
      }),
    ).toBe("develop");
  });

  it("uses explicit baseRef when it matches options", () => {
    expect(
      resolveBaseRef(workspace, {
        workspaceId: "demo",
        mode: "plan_only",
        prompt: "hello",
        baseRef: "main",
        options: { baseRef: "main" },
      }),
    ).toBe("main");
  });

  it("rejects conflicting baseRef values", () => {
    expect(() =>
      resolveBaseRef(workspace, {
        workspaceId: "demo",
        mode: "plan_only",
        prompt: "hello",
        baseRef: "main",
        options: { baseRef: "develop" },
      }),
    ).toThrow(InvalidRunOptionsError);
  });

  it("rejects invalid option values", () => {
    expect(() =>
      validateRunOptions(workspace, { baseRef: "not-a-choice" }),
    ).toThrow(InvalidRunOptionsError);
  });
});
