---
name: skill-creator
description: Create new skills, modify and improve existing skills for AgentRunr. Use when users want to create a skill from scratch, edit or improve an existing skill, or refine a skill's description so it triggers more reliably. Also use when a user asks to "turn this workflow into a skill", "save these instructions as a skill", or "make the agent better at X".
---

# Skill Creator

A skill for creating and iteratively improving AgentRunr skills.

Skills in AgentRunr live at `workspace/skills/<skill-name>/SKILL.md` and are loaded dynamically by the `SkillsTool` at runtime. The agent picks them up automatically — no restart needed.

The core loop:
1. Understand what the skill should do
2. Write a draft `SKILL.md`
3. Walk through 2-3 test scenarios and evaluate the output inline
4. Refine based on feedback
5. Repeat until the user is happy

Jump in wherever the user is. If they already have a draft, skip straight to testing. If they just have a vague idea, interview them first.

---

## Creating a skill

### Capture Intent

Start by understanding what the user actually wants. If the conversation already shows a workflow the user wants to capture (a sequence of steps, tools used, corrections made), extract the answers from that — then fill gaps with the user.

Key questions:
1. What should this skill enable the agent to do?
2. When should it trigger? (what kinds of user messages)
3. What does a good output look like?

### Interview

Ask about edge cases, expected inputs, success criteria, and any context the agent will need. Don't start writing until you have enough to write something useful — but don't over-interview either. If you can make a reasonable assumption, make it and note it.

### Write the SKILL.md

Create `workspace/skills/<skill-name>/SKILL.md` with:

- **`name`**: kebab-case identifier matching the directory name
- **`description`**: The primary triggering mechanism. Include _what_ the skill does AND _when_ to use it. The agent decides whether to load a skill based solely on this field — so be specific and make it slightly "pushy". Instead of "Helps with data analysis", write "Helps with data analysis. Use this skill whenever the user asks about datasets, CSV files, charts, or wants to understand or transform data — even if they don't say 'analysis'."
- **Body**: Step-by-step instructions for what the agent should do. Explain the _why_ behind important steps so the agent can apply judgment, not just follow rules mechanically.

#### Anatomy of a skill

```
workspace/skills/
└── skill-name/
    └── SKILL.md   ← required; keep under ~300 lines
```

AgentRunr skills are single-file. The agent reads `SKILL.md` when the skill triggers. Keep it focused and readable — a dense wall of text is harder to follow than clear, structured instructions.

#### Writing patterns

Use imperative form. Explain reasoning where it matters.

**Output format example:**
```markdown
## Report structure
Use this template every time:
# [Title]
## Summary
## Details
## Next steps
```

**Example pattern:**
```markdown
## Commit message format
User said: "Added login with Google"
Write: feat(auth): add Google OAuth login
```

#### Principles

- Lean over comprehensive. A shorter, well-reasoned skill beats a long checklist.
- Explain the *why* so the agent can adapt to situations the skill didn't explicitly anticipate.
- Avoid ALL CAPS MUSTs and rigid structures where possible — trust the agent's judgment when given good context.
- Skills must not contain malware, exploit code, or content that would surprise the user given the skill's stated purpose.

---

## Testing the skill

After writing a draft, come up with 2-3 realistic test prompts — things a real user would actually say. Share them and confirm with the user before proceeding.

For each test prompt, read the skill and follow its instructions yourself to complete the task. Present the output to the user and ask for feedback:

> "Here's what the agent would do for: _[prompt]_. Does this look right? Anything you'd change?"

This is intentionally lightweight — you wrote the skill and you're running it, so you have full context. The goal is a quick sanity check, not a rigorous benchmark. The human review is what matters.

---

## Improving the skill

After the user reviews, update the skill based on their feedback. A few principles:

1. **Generalize, don't patch.** If a test case revealed a gap, think about what the underlying issue is and fix that — don't just add a special case for the exact example.

2. **Stay lean.** Remove instructions that aren't pulling their weight. If the agent is already doing something naturally, you don't need to spell it out.

3. **Explain the why.** If you find yourself adding a rigid rule, ask whether you could instead explain the reasoning so the agent understands _why_ and can apply it flexibly.

After updating, re-run the same test prompts (and any new ones) and repeat until:
- The user is satisfied
- There's nothing more to improve

---

## Description optimization

The `description` field is the only thing the agent sees when deciding whether to load a skill. A weak description means the skill never triggers; an overly broad one means it triggers when it shouldn't.

After finishing the skill, review the description with the user:

1. Identify 3-5 kinds of user messages that _should_ trigger this skill — including indirect ones that don't name the skill explicitly.
2. Identify 2-3 near-miss cases — messages that share keywords but actually need something different.
3. Revise the description to clearly cover the should-trigger cases and implicitly exclude the near-misses.

Present before/after to the user and confirm.

---

## Updating an existing skill

If the user wants to improve an existing skill rather than create a new one:
- Read the current `SKILL.md` first before suggesting any changes.
- Keep the skill's `name` and directory unchanged.
- Apply the same test → feedback → refine loop as above.

---