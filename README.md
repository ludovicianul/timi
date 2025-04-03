# ⏱️ Timi — Time Tracking CLI

**Timi** (`time` in Old Norse) is a blazing-fast, local-first command-line tool to track how you spend your time. It's designed for developers and productivity-minded professionals
who prefer flexibility, transparency, and full ownership of their data.

---

## 🚀 Quick Start

### 1. Install

#### Option 1: Homebrew (macOS)

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
./target/timi --help
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
| `edit`                | Modify an existing entry                   |
| `delete`              | Delete an entry by ID                      |
| `list`                | List entries with filters and tag support  |
| `stats`               | View summary statistics                    |
| `timeline`            | Visualize daily, weekly, or monthly effort |
| `analyze`             | Analyze patterns, context switching, etc.  |
| `search`              | Search entries by tag, type, or note       |
| `notes`               | View notes associated with time logs       |
| `config`              | Manage tags and activity types             |
| `index`               | Inspect, rebuild, or validate index        |
| `info`                | Show app/system version and paths          |
| `generate-completion` | Generate shell autocompletion script       |

Run `timi [command] --help` for details.

---

## 🎨 Shell Autocomplete

Enable Bash/Zsh/Fish autocomplete:

```bash
timi generate-completion > timi_completion
source timi_completion
```

---

## 📂 Data Structure

| File                           | Purpose                |
|--------------------------------|------------------------|
| `~/.timi/config.json`          | Tags and types config  |
| `~/.timi/index.json`           | Entry UUID → file map  |
| `~/.timi/entries/YYYY-MM.json` | Time entries per month |

---

## 📦 Features

- ⚡ Add, edit, delete entries
- 🏷️ Tag and categorize activities
- 📊 Timeline and statistical analysis
- 🧠 Deep vs shallow work insights
- 📎 JSON data, Git versioned (optional)

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

---

## 🛠 Requirements

- Java 17+
- Maven (for building)
- Optional: GraalVM for native image support

---

## 📄 License

MIT © 2025 Madalin Ilie
