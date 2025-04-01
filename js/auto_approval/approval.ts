'use strict';

import * as github from '@actions/github';
import * as core from '@actions/core';
import { WebhookPayload } from '@actions/github/lib/interfaces.js';

export interface CommandLineInputs {
  token: string;
  allowed_review_for: string;
  review_as: string;
}

export interface ActionInputs {
  token: string;
  allowed_review_for: string[];
  review_as: string;
  sender_login: string;
  requested_reviewers: string[];
  source_git: string;
  target_git: string;
}

export function getActionInputs(commandLineInputs: CommandLineInputs, githubPayload: WebhookPayload): ActionInputs {
  const pullRequest = githubPayload.pull_request!;
  return {
    token: commandLineInputs.token,
    review_as: commandLineInputs.review_as,
    allowed_review_for: commandLineInputs.allowed_review_for
      .split(',')
      .map((u) => u.trim())
      .filter((u) => u.length > 0),
    sender_login: pullRequest.user.login,
    requested_reviewers: pullRequest.requested_reviewers
      .map((u: { login: string }) => u.login)
      .filter((u: string) => u.length > 0),
    source_git: pullRequest.base.git_url,
    target_git: pullRequest.head.git_url,
  };
}

export function shouldApprove(actionInputs: ActionInputs): boolean {
  if (actionInputs.source_git === actionInputs.target_git) {
    // required, since we can only get the secret from when running in our repo context.
    core.info(`PR originated from the target git repo, we can review this.`);
    if (actionInputs.requested_reviewers.includes(actionInputs.review_as)) {
      core.info(`'${actionInputs.review_as}' has been requested to review.`);
      if (actionInputs.allowed_review_for.includes(actionInputs.sender_login)) {
        core.info(`User '${actionInputs.sender_login}' PR will be approved.`);
        return true;
      } else {
        core.info(
          `User '${actionInputs.sender_login}' is not in allowed list: ${actionInputs.allowed_review_for.join(
            ', ',
          )}. PR will not be auto-approved.`,
        );
        return false;
      }
    } else {
      core.info(
        `'${actionInputs.review_as}' is not in list of requested reviewers: ${actionInputs.requested_reviewers.join(
          ', ',
        )}. PR will not be auto-approved.`,
      );
      return false;
    }
  } else {
    core.info(
      `PR repo is ${actionInputs.source_git}, which is not our repo ${actionInputs.target_git}. We are not allowed to get API token in such context.`,
    );
    return false;
  }
}

export async function approvePr(token: string): Promise<void> {
  const octokit = github.getOctokit(token);

  await octokit.rest.pulls.createReview({
    ...github.context.repo,
    pull_number: github.context.payload.number,
    event: 'APPROVE',
  });
}
