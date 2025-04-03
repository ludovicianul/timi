# 📚 Command Reference for `timi`

This document describes all available commands in the `timi` CLI tool.
For full syntax, use `--help` with any command.

---

## 🟢 `add`

Add a new time entry.

```bash
timi add --start "2025-04-01T09:00" --duration 60 --type work --tags projectX --note "Updated docs"
timi add --interactive
```

**Options:**

- `--start`, `-s` – ISO timestamp (default: now)
- `--duration`, `-d` – Duration in minutes **(required)**
- `--type`, `-t` – Activity type **(required)**
- `--tags` – Comma-separated list of tags
- `--note`, `-n` – Optional description
- `--interactive`, `-i` – Guided entry

---

## 🟡 `edit`

Edit an existing time entry.

```bash
timi edit --id <UUID> --note "Revised description"
timi edit --id <UUID> --interactive
```

**Options:**

- `--id`, `-i` – Entry ID **(required)**
- `--start`, `--duration`, `--type`, `--tags`, `--note`
- `--interactive` – Prompts for each field

---

## 🔴 `delete`

Delete a time entry by ID.

```bash
timi delete --id <UUID>
```

**Options:**

- `--id` – UUID of entry to delete
- Confirmation prompt included

---

## 📋 `list`

List time entries within a date range.

```bash
timi list --from 2025-04-01 --to 2025-04-07 --show-tags
```

**Options:**

- `--month`, `-m` – Shortcut for monthly view
- `--from`, `--to` – Date range
- `--only-tag` – Filter by tag
- `--show-ids`, `--show-tags` – Include IDs/tags in output

---

## 📈 `stats`

View summarized statistics.

```bash
timi stats --from 2025-04-01 --to 2025-04-30 --group-by tag
```

**Options:**

- `--day`, `--from`, `--to`
- `--group-by` – `tag` or `type`
- `--daily-breakdown` – Includes daily totals

---

## 📊 `timeline`

Visualize effort per day/week/month.

```bash
timi timeline --from 2025-01-01 --to 2025-03-31 --view month
```

**Options:**

- `--from`, `--to` – Required range
- `--view` – `day`, `week`, or `month`

---

## 🧠 `analyze`

Analyze context switching, focus, and usage.

```bash
timi analyze --context-switch --by week
```

**Options:**

- `--context-switch`, `--peak`, `--focus-score`, `--dow-insights`
- `--by` – `day` or `week`
- `--target` – Tag or activity type

---

## 🔎 `search`

Search entries by tag, type, or note.

```bash
timi search --tag java --note "meeting" --summary
```

**Options:**

- `--tag`, `--activity`, `--note`
- `--from`, `--to`, `--summary`

---

## 📝 `notes`

View notes for a tag on a specific day or month.

```bash
timi notes --tag clientA --month 2025-03
```

**Options:**

- `--tag` – Required
- `--day`, `--month`

---

## ⚙️ `config`

Manage tags and activity types.

```bash
timi config add-tag --name personal
```

Subcommands:

- `add-tag`, `add-type`, `remove-tag`, `remove-type`
- `show-tags`, `show-types`, `list`, `validate`, `prune`

---

## 🧭 `index`

Manage the entry index.

```bash
timi index rebuild
```

Subcommands:

- `rebuild` – Full index rebuild
- `validate` – Check for consistency
- `show` – Print all mappings

---

## 📦 `info`

Print version and system info.

```bash
timi info
```

Shows:

- App version
- Java version
- Config/data paths

---

## 🧪 `generate-completion`

Generate shell completion scripts.

```bash
timi generate-completion > timi_completion
```

Supports Bash, Zsh, Fish.
