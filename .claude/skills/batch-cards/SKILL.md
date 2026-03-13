---
name: batch-cards
description: Implement multiple random cards from a set backlog in sequence, each in an isolated sub-agent with fresh context.
argument-hint: <count> <path-to-backlog-cards.md>
---

# Batch Implement Random Cards

Implement multiple random cards from a backlog by spawning sequential sub-agents, each with fresh context.

## Parse Arguments

Parse `$ARGUMENTS` for:
- **count** (required): Number of cards to implement (integer, e.g., `3`)
- **backlog path** (required): Path to the backlog markdown file (e.g., `backlog/sets/scourge/cards.md`)

If either is missing, ask the user.

## Execution Loop

For each card (1 through count):

1. **Announce**: Print `## Card {i}/{count}` so the user can track progress.

2. **Spawn a sub-agent** using the Agent tool with these settings:
   - **isolation**: `"worktree"` — each agent works on an isolated copy of the repo to avoid conflicts
   - **prompt**: Tell the agent to run the `add-random-card` skill with the backlog path as argument. Include the full instruction:
     ```
     Run the add-random-card skill: /add-random-card <backlog-path>

     IMPORTANT:
     - Use the Skill tool to invoke the skill: skill = "add-random-card", args = "<backlog-path>"
     - Do NOT pick any of these cards (already being implemented in this batch): <list of cards picked so far>
     - Commit your changes when done.
     ```
   - **run_in_background**: `false` — wait for each agent to complete before starting the next (sequential execution prevents merge conflicts on shared files like the set registration and backlog)

3. **Record result**: Note which card the agent implemented (from its response). Add it to the exclusion list for subsequent agents.

4. **Merge the worktree**: After each agent completes, if it made changes in a worktree branch, merge that branch into the current branch:
   ```bash
   git merge <worktree-branch> --no-edit
   ```
   If there are merge conflicts, stop and report them to the user.

5. **Repeat** for the next card.

## After All Cards

Print a summary table:

```
## Batch Complete

| # | Card Name | Status |
|---|-----------|--------|
| 1 | Card A    | Done   |
| 2 | Card B    | Done   |
| 3 | Card C    | Failed |
```

Report any failures. Do NOT push to remote.
