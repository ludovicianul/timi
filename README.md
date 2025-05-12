# ⏱️ Timi — Time Tracking CLI

**Timi** (`time` in Old Norse) is a blazing-fast, local-first command-line tool to track how you spend your time. It's designed for developers and productivity-minded professionals
who prefer flexibility, transparency, and full ownership of their data.

---

## 🚀 Quick Start

### 1. Install

#### Option 1: Homebrew (macOS)

```bash
brew install ludovicianul/tap/timi
```

#### Option2: Build from sources

```bash
git clone https://github.com/ludovicianul/timi.git
cd timi
./mvnw package
java -jar ./target/timi-runner.jar --help
```

Optional (native build):

```bash
./mvnw package -Pnative
cp target/timi-runner timi
./timi --help
```

---

### 2. Add a Time Entry

```bash
timi add --start "2025-04-01T09:00" \
          --duration 90 \
          --type coding \
          --tags projectX \
          --note "Implemented new analytics module"
```

Or use:

```bash
timi add --interactive
```

---

## 🔍 Commands Overview

| Command               | Description                                |
|-----------------------|--------------------------------------------|
| `add`                 | Add a new time entry                       |
| `audit`               | Audit entries for common issues            |
| `abort`               | Abort the current session without saving   |
| `config`              | View or modify configuration               |
| `delete`              | Delete an entry by ID                      |
| `dashboard`           | Show a daily or monthly dashboard          |
| `edit`                | Modify an existing entry                   |
| `export`              | Export time entries to CSV or JSON format  |
| `batch`               | Perform batch operations on time entries   |
| `undo`                | Undo the last action (add/edit/delete)     |
| `last`                | Show the last action that can be undone    |
| `list`                | List entries with filters and tag support  |
| `stats`               | View summary statistics                    |
| `timeline`            | Visualize daily, weekly, or monthly effort |
| `analyze`             | Analyze patterns, context switching, etc.  |
| `search`              | Search entries by tag, type, or note       |
| `notes`               | View notes associated with time logs       |
| `config`              | Manage tags and activity types             |
| `index`               | Inspect, rebuild, or validate index        |
| `info`                | Show app/system version and paths          |
| `template`            | Define templates                           |
| `info`                | Show app/system version and paths          |
| `zen`                 | Reflect on your focus and balance          |
| `pause`               | Pause the current session                  |
| `resume`              | Resume a paused session                    |
| `start`               | Start a new work session                   |
| `stop`                | Stop and save the current session          |
| `status`              | Check current session status               |
| `generate-completion` | Generate shell autocompletion script       |

Run `timi [command] --help` for details.

More details here: [COMMANDS.md](./docs/COMMANDS.md)

---

## 🎨 Shell Autocomplete

Enable Bash/Zsh/Fish autocomplete:

```bash
timi generate-completion > timi_completion
source timi_completion
```

---

## 📂 Data Structure

| File                           | Purpose                 |
|--------------------------------|-------------------------|
| `~/.timi/config.json`          | Tags and types config   |
| `~/.timi/index.json`           | Entry UUID → file map   |
| `~/.timi/session.json`         | Current session details |
| `~/.timi/zen-suggestions.json` | Custom zen suggestions  |
| `~/.timi/entries/YYYY-MM.json` | Time entries per month  |
| `~/.timi/templates/NAME.json`  | Templates               |

---

## 📦 Features
•	⚡ Track work sessions in real-time: start, pause, resume, stop
•	🧭 Session-aware time tracking with automatic rounding (0, 5, 10 min)
•	🧠 Deep vs Shallow Work insights with daily/weekly zen summaries
•	🧘 Personalized Zen reflections driven by persona and mood
•	🏷️ Tag and categorize activities with validation and suggestions
•	📊 Timeline and statistical analysis with day/week/month visualizations
•	🔎 Dashboard for today/month: compact view of focus, effort, and audit
•	📎 Template support for quick reusable entries
•	📋 Audit mode to catch short durations, missing notes, and overuse patterns
•	⏱ Undo/redo actions for add/edit/delete with full recovery
•	📁 Configurable behavior: rounding, themes, reminder toggles, and more
•	🧾 Session logs and status: current duration, paused state, metadata
•	💾 Data saved as JSON, optionally Git-versioned
---

## 💡 Examples

```bash
# Weekly timeline
$ timi timeline --from 2025-04-01 --to 2025-04-07 --view week

# Analyze most context-switched weeks
timi analyze --context-switch --by week

# Rebuild index
timi index rebuild
```

More examples here: [EXAMPLES.md](./docs/EXAMPLES.md)

---

## 🛠 Requirements

- Java 21+ (if using uberjar)
- Maven (for building)
- Optional: GraalVM for native image build

---

## 📄 License

MIT © 2025 Madalin Ilie
