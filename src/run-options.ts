import type { CreateRunRequest } from "./api-schema.js";
import type { Workspace } from "./config-schema.js";
import { InvalidRunOptionsError } from "./errors.js";
import { gitRefSchema } from "./security/git-ref.js";

export function resolveBaseRef(
  workspace: Workspace,
  request: CreateRunRequest,
): string | undefined {
  validateRunOptions(workspace, request.options);

  const optionBaseRef = request.options?.baseRef;
  if (request.baseRef && optionBaseRef && request.baseRef !== optionBaseRef) {
    throw new InvalidRunOptionsError("Conflicting baseRef in request and options");
  }

  const baseRef = request.baseRef ?? optionBaseRef;
  if (baseRef !== undefined) {
    gitRefSchema.parse(baseRef);
  }

  return baseRef;
}

export function validateRunOptions(
  workspace: Workspace,
  options: Record<string, string> | undefined,
): void {
  if (!workspace.runOptions?.length) {
    return;
  }

  for (const group of workspace.runOptions) {
    const value = options?.[group.key];
    if (group.required && !value) {
      throw new InvalidRunOptionsError(`Missing required option: ${group.key}`);
    }
    if (value && !group.choices.some((choice) => choice.value === value)) {
      throw new InvalidRunOptionsError(`Invalid option value for: ${group.key}`);
    }
  }
}
