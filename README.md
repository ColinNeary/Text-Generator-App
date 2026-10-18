# Text Generator AI

A JavaFX desktop app that learns word patterns from text files and uses them to autocomplete and generate sentences. You import a book or document, the app records every word and word pair in MySQL, and then generates text with three algorithms:

- **Greedy:** always picks the most likely next word.
- **Markov chain:** picks the next word at random, weighted by how often each word pair appears.
- **Beam search:** keeps several candidate sentences at once and returns the most probable one.

Built by Group 33 for CS 4485 (Senior Project) at UT Dallas, Fall 2025. This repo preserves the final version of the team's project and its commit history.

## What you need

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | 17 or newer | Any distribution works (Temurin, Oracle, or one IntelliJ downloads). |
| MySQL Server | 8.x | MySQL Workbench is the easiest way to run the setup script. |
| Maven | none | The included Maven wrapper (`mvnw`) downloads it for you. |

## Setup (one time)

### 1. Create the database

1. Open MySQL Workbench and connect to your local server.
2. Go to **File → Open SQL Script** and choose `database/setup.sql`.
3. Click the lightning-bolt button to run the whole script.
4. Refresh the **Schemas** panel and check that `team33db` appears.

The script creates the `files`, `words` and `word_pairs` tables. The app creates everything else it needs the first time it runs.

### 2. Tell the app how to log in

Copy `db.properties.example` to a new file named `db.properties` in the project root, then fill in your MySQL password:

```properties
db.url=jdbc:mysql://localhost:3306/team33db
db.user=root
db.password=your-mysql-password
```

`db.properties` is in `.gitignore`, so your password never gets committed. If your server uses a different port or user, change `db.url` or `db.user`.

### 3. Point the build at JDK 17+

Maven uses whichever JDK `JAVA_HOME` points to. Check it with `java -version`. If it shows something older than 17, point `JAVA_HOME` at a newer JDK for the current terminal:

**Windows (PowerShell)**

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"
```

**macOS / Linux**

```bash
export JAVA_HOME=/path/to/jdk-17
```

## Running the app

From the project root:

**Windows**

```powershell
.\mvnw.cmd javafx:run
```

**macOS / Linux**

```bash
./mvnw javafx:run
```

The first run downloads dependencies and takes a minute or two. After that it starts in a few seconds.

**From IntelliJ:** open the project folder, set the project SDK to JDK 17+ under **File → Project Structure**, then run `HelloApplication`. The working directory must be the project root so the app can find `db.properties`.

## Using the app

1. **Import text first.** Every feature works from imported data, so the other screens stay empty until you do this. Open **Import File** and choose a `.txt`, `.docx` or `.pdf` file. `datasets/pride_and_prejudice_clean.txt` is a good starter. A full book takes a little while, and a progress bar shows how far along it is.
2. **Auto Complete:** type or pick a starting word, then build a sentence one word at a time. **Greedy** suggests the most likely next words, and **Markov** suggests weighted random ones. Click a suggestion to add it, then **Finish** or **Reset**.
3. **Sentence Generation:** enter a starting word, then click **Generate with Greedy** or **Generate with Beam Search** to generate a whole sentence.

Importing more files adds to the existing counts. To start over, truncate the tables in Workbench or rerun `database/setup.sql` on a fresh schema.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `Unknown database 'team33db'` | Run `database/setup.sql` (setup step 1). |
| `Access denied for user 'root'` | The password in `db.properties` is wrong, or the file is missing. |
| `db.properties not found` in the console | The app wasn't started from the project root. Run it from there, or set IntelliJ's working directory. |
| `Communications link failure` | MySQL isn't running. On Windows, start the **MySQL80** service from Services. |
| Build error about `release 17` or `invalid target release` | `JAVA_HOME` points to an older JDK (setup step 3). |
| Generated sentences are empty | Nothing has been imported yet, or the starting word never appears in the imported text. |

## Project layout

```
src/main/java/com/example/
├── backend/
│   ├── DbConfig.java                 # reads db.properties
│   ├── DatabaseManager.java          # word/pair queries, POS tagging
│   ├── autocomplete/                 # Greedy and Markov next-word suggestions
│   └── sentencegen/                  # Greedy and beam search sentence generation
└── javafxproject/                    # JavaFX screens, controllers and file import
src/main/resources/.../*.fxml         # screen layouts and styles.css
database/setup.sql                    # creates team33db
Schema & Query/                       # original schema dump and example queries
Schema Diagrams & Tables/             # ER diagram, grammar diagrams, sample data
datasets/                             # sample text to import
```
