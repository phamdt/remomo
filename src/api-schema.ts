import { z } from "zod";
import { gitRefSchema } from "./security/git-ref.js";

export const runModeSchema = z.enum(["plan_only", "apply"]);

export const createRunRequestSchema = z.object({
  workspaceId: z.string().min(1),
  mode: runModeSchema,
  prompt: z.string().min(1).max(100_000),
  baseRef: gitRefSchema.optional(),
  options: z.record(z.string(), z.string()).optional(),
});

export const continueRunRequestSchema = z
  .object({
    prompt: z.string().min(1).max(100_000).optional(),
    mode: runModeSchema.optional(),
  })
  .superRefine((data, ctx) => {
    if (!data.prompt && data.mode !== "apply") {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "prompt is required unless mode is apply",
      });
    }
  });

export const listRunsQuerySchema = z.object({
  limit: z.coerce.number().int().min(1).max(100).default(20),
  workspaceId: z.string().min(1).optional(),
});

export type CreateRunRequest = z.infer<typeof createRunRequestSchema>;
export type ContinueRunRequest = z.infer<typeof continueRunRequestSchema>;
export type ListRunsQuery = z.infer<typeof listRunsQuerySchema>;
