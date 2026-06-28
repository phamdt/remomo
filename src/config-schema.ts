import path from "node:path";
import { z } from "zod";
import { gitRefSchema } from "./security/git-ref.js";

export const workspaceRepoPathSchema = z
  .string()
  .min(1)
  .max(100)
  .regex(/^[a-zA-Z0-9._-]+$/, "Invalid repo path")
  .refine((value) => !value.includes(".."), "Invalid repo path")
  .refine((value) => !path.isAbsolute(value), "Invalid repo path");

export const repoSchema = z.object({
  id: z.string().min(1),
  name: z.string().min(1),
  url: z.string().min(1),
  defaultBranch: gitRefSchema,
  enabled: z.boolean(),
});

export const workspaceRepoSchema = z.object({
  repoId: z.string().min(1),
  role: z.string().min(1),
  path: workspaceRepoPathSchema,
});

export const runOptionChoiceSchema = z.object({
  value: z.string().min(1),
  label: z.string().min(1),
});

export const runOptionGroupSchema = z.object({
  key: z.string().min(1),
  label: z.string().min(1),
  required: z.boolean().default(false),
  choices: z.array(runOptionChoiceSchema).min(1),
});

export const workspaceSchema = z.object({
  id: z.string().min(1),
  name: z.string().min(1),
  repos: z.array(workspaceRepoSchema).min(1),
  defaultPromptContext: z.string().optional(),
  runOptions: z.array(runOptionGroupSchema).optional(),
});

export const reposConfigSchema = z.array(repoSchema).min(1);
export const workspacesConfigSchema = z.array(workspaceSchema).min(1);

export type Repo = z.infer<typeof repoSchema>;
export type Workspace = z.infer<typeof workspaceSchema>;
