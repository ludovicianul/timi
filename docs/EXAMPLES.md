# 💡 Usage Examples for `timi`

This guide includes real-world usage scenarios to help you get the most out of `timi`.

---

## 🟢 Adding Entries

### Add an entry manually:

```bash
timi add \
  --start "2025-04-01T09:00" \
  --duration 90 \
  --type coding \
  --tags projectX,java \
  --note "Implemented authentication module"
```

### Use interactive entry mode:

```bash
timi add --interactive
```

---

## 🟡 Editing Entries

### Edit just the note:

```bash
timi edit --id <UUID> --note "Clarified scope of work"
```

### Full guided edit:

```bash
timi edit --id <UUID> --interactive
```

---

## 🔴 Deleting Entries

### Delete an entry by ID:

```bash
timi delete --id <UUID>
```

---

## 📋 Listing and Filtering

### List entries for a week:

```bash
timi list --from 2025-04-01 --to 2025-04-07 --show-tags --show-ids
```

### List only entries with a specific tag:

```bash
timi list --only-tag personal
```

---

## 📈 Statistics

### Show time breakdown by tag for April:

```bash
timi stats --from 2025-04-01 --to 2025-04-30 --group-by tag
```

### Include daily breakdown:

```bash
timi stats --from 2025-04-01 --to 2025-04-07 --group-by type --daily-breakdown
```

---

## 📊 Timeline Visualization

### Daily timeline (aggregated):

```bash
timi timeline --from 2025-04-01 --to 2025-04-07 --view day
```

### Monthly effort chart:

```bash
timi timeline --from 2025-01-01 --to 2025-04-30 --view month
```

---

## 🧠 Analysis

### Most context-switched days:

```bash
timi analyze --context-switch --by day
```

### Focus score across the month:

```bash
timi analyze --focus-score
```

### Day-of-week patterns:

```bash
timi analyze --dow-insights
```

---

## 🔎 Search & Notes

### Search by note content:

```bash
timi search --note "meeting" --summary
```

### View notes for a tag in March:

```bash
timi notes --tag clientA --month 2025-03
```

---

## ⚙️ Config & Index

### Add a new tag and activity type:

```bash
timi config add-tag --name exercise
timi config add-type --name break
```

### Rebuild and validate index:

```bash
timi index rebuild
timi index validate
```

---

## 📦 Utility

### View current version and system info:

```bash
timi info
```

### Generate shell autocompletion:

```bash
timi generate-completion > timi_autocomplete
source timi_autocomplete
```

---

Want more? See the full [COMMANDS.md](./COMMANDS.md) for all CLI options.

