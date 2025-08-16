# FTB Quests Lang Splitter

## 📰 Introduction

This mod adds a feature to split lang files for better version control, also adds some utility commands.

## 📦 Installation

Mod is available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ftb-quests-lang-splitter).

## 🔧 Features

### 👨‍💻 Commands

- `split`

Splits the lang file into their own category type and for quests, tasks and quest links they are grouped by chapters.
```css
lang/
├─ en_us/
│  ├─ chapters/
│  │  ├─ chapter1.snbt # Contains quest, task and quest links ids
│  │  ├─ chapter2.snbt
│  │  └─ chapter3.snbt
│  ├─ chapter.snbt # Chapter names
│  ├─ chapter_group.snbt # Chapter group names
│  ├─ file.snbt # Quest book name
│  └─ reward_table.snbt # Reward table names
└─ en_us.snbt # Original file, it updates from split files when loaded/reloaded
```
`replace_unmerged: boolean (you mostly want to use false, unless you know you want to overwrite the previously generated and non-merged split files)`

`locale: locale code (ie: pt_br, zh_ch, ko_kr)`

```bash
/langsplitter split <replace_unmerged> [<locale>]
# If you don't provide a locale it will split every lang available.
```
- `fill_missing_translation`

Fills missing lang entries based on `en_us` locale and runs the split.

```bash
/langsplitter fill_missing_translation <replace_unmerged> <locale>
```

- `purge_merged`

When you load or reload the quest and there are available split files to be read, they are merged into the original file and after that renamed to .snb_merged. You can use this command to purge them from the folder if you don't like seeing those.

```bash
/langsplitter purge_merged
```

### Usage

- Setup your .gitignore:
```gitignore
# FTB Quests Lang Splitter
# ignore original lang files like en_us.snbt
config/ftbquests/quests/lang/*.snbt

# ignore recovery files
config/ftbquests/quests/lang/recovery/*.snbt

# ignore already merged files
config/ftbquests/quests/lang/**/*.snbt_merged
```
- For translators:
  - start with the `fill_missing_translation` command to generate the files containing all entries.
  - translate what you need
  - run `/ftbquests reload` to merge your changes
  - now your files will be renamed to .snbt_merged
  - if you still need to do more changes you can either:
    - run `split` command again to generate the file
    - or edit the now `.snbt_merged` and after done, rename back to `.snbt`
    - and now reload again to merge new changes
  - if everything is ready to be commited, just run `split` one last time and PR the relevant files (run `purge_merged` if they are on the way, but a proper .gitignore should deal with it).

- For maintainers:
  - update the .gitignore from your repo
  - now the /lang/ folder should only contain locale folders, not files anymore.
  - to generate those starting files run `split` command and commit the files.
  - after the folders are set, now you can remove the original locale .snbt file (those are auto-generated now when merged from split files at runtime)
  - for now on, only accept PRs/commits that are from split files.

- About the `recovery` folder:
  - when files are merged and they replace an existing entry like when you translate again an entry, it will backup that entry at folder `recovery` containing the following:
  - a file with name like `en_us_<timestamp>.snbt` will be generated (ie: `en_us_1755120819.snbt`)
```json
  {
    "chapters/basic_armor.snbt": {
       quest.004D61425172324F.quest_subtitle: {
         new: "This is my new subtitle"
         old: "This is the old subtitle"
       }
    }
  }
```
  

## 📝 License

This project is licensed under the MIT License.

## 📬 Contact

You can reach me on [All The Mods Discord](https://discord.gg/allthemods) with `@Uncandango`, you can also open your issue on [Github](https://github.com/pietro-lopes/FTB-Quests-Lang-Splitter/issues) or comment on the mod comment's section on CurseForge.

## 👍 Sponsors

<div><a href="https://www.yourkit.com/" rel="nofollow"><img src="https://www.yourkit.com/images/yklogo.png" width="100"></a></div>

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/dotnet-profiler/), and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

***
