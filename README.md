# gettext-maven-plugin

`gettext-maven-plugin` integrates the GNU gettext toolchain into a Maven
build. It extracts translatable messages from Java source files, updates PO
translation catalogs, generates Java resource bundles, and produces a
translation statistics report.

Project home: <https://github.com/cnhongwei/gettext-maven-plugin>

The plugin is published to Maven Central and can be found at:
[Maven Central](https://central.sonatype.com/artifact/io.github.cnhongwei/gettext-maven-plugin)

## Requirements

- Java 8 or later
- Maven
- GNU gettext command-line tools on `PATH`:
  - `xgettext` for extracting messages
  - `msgmerge` for updating PO files
  - `msgfmt` for compiling catalogs and collecting statistics
  - `msgcat` when generating Java properties files

## Maven configuration

Add the plugin to the project build. Replace `VERSION` with the version you
want to use.

```xml
<plugin>
  <groupId>io.github.cnhongwei</groupId>
  <artifactId>gettext-maven-plugin</artifactId>
  <version>VERSION</version>
  <configuration>
    <poDirectory>${project.basedir}/src/main/po</poDirectory>
    <keysFile>keys.pot</keysFile>
    <sourceLocale>en</sourceLocale>
    <targetBundle>com.example.Messages</targetBundle>
    <outputFormat>properties</outputFormat>
  </configuration>
</plugin>
```

The default PO directory is `src/main/po`, and the default source locale is
`en`. `sourceLocale` identifies the language of the source catalog in the
console statistics and is also used by `dist` when creating the source
resource bundle.

## Extracting messages from additional source files

By default, the `gettext` goal scans Java files under `sourceDirectory`. Use
`extraSourceFiles` to include files from another directory, such as JSP
templates. Includes and excludes are supported.

```xml
<extraSourceFiles>
  <directory>${project.basedir}/src/main/webapp</directory>
  <includes>
    <include>**/*.jsp</include>
  </includes>
  <excludes>
    <exclude>**/generated/**</exclude>
  </excludes>
</extraSourceFiles>
```

The configured files are extracted together with the Java sources into the
same POT catalog. The extra directory may be different from
`sourceDirectory`.

## Goals

The plugin provides these goals:

| Goal | Description |
| --- | --- |
| `gettext` | Extract messages from Java source files into the POT catalog. |
| `merge` | Merge the POT catalog into existing PO files. |
| `dist` | Compile PO files into Java resource bundles. |
| `report` | Generate the HTML gettext statistics report. |

This plugin does not provide goals named `extract` or `compile`. The
equivalent operations are named `gettext` and `dist` here.

Run the goals using the full plugin coordinates:

```bash
mvn io.github.cnhongwei:gettext-maven-plugin:gettext
mvn io.github.cnhongwei:gettext-maven-plugin:merge
mvn io.github.cnhongwei:gettext-maven-plugin:dist
mvn io.github.cnhongwei:gettext-maven-plugin:report
```

The `merge` goal does not invoke `gettext` automatically. Run extraction
first when the POT catalog needs to be updated.

## Recommended workflow

```bash
# Extract messages from the source code.
mvn io.github.cnhongwei:gettext-maven-plugin:gettext

# Update existing translations with the latest source messages.
mvn io.github.cnhongwei:gettext-maven-plugin:merge

# Edit and review the PO files in src/main/po.

# Generate Java resource bundles.
mvn io.github.cnhongwei:gettext-maven-plugin:dist

# Generate the HTML report when required.
mvn io.github.cnhongwei:gettext-maven-plugin:report
```

## Translation statistics

After `gettext`, `merge`, `dist`, and `report`, the plugin prints a translation
coverage table to the Maven console. The source catalog is marked with the
configured source locale.

```text
┌─────────────┬─────────────┬─────────┐
│ Language    │ Total count │ Missing │
├─────────────┼─────────────┼─────────┤
│ th          │        1293 │       0 │
│ zh (source) │        1169 │       - │
└─────────────┴─────────────┴─────────┘
```

The columns are calculated as follows:

- `Total count` = translated + fuzzy + untranslated messages
- `Missing` = fuzzy + untranslated messages
- The source catalog has no missing-translation value, so it is shown as `-`

A fuzzy entry contains a translation that gettext considers a possible match,
but that has not been confirmed by a translator. Fuzzy entries are therefore
counted as missing until they are reviewed and confirmed.

Statistics are collected with `msgfmt --statistics`. If the source catalog or
an external gettext command is unavailable, the plugin reports a warning and
keeps the existing goal behavior.

## Common configuration parameters

| Parameter | Default | Description |
| --- | --- | --- |
| `poDirectory` | `${project.basedir}/src/main/po` | Directory containing the POT and PO files. |
| `keysFile` | `keys.pot` | Source POT catalog filename. |
| `sourceLocale` | `en` | Locale of the source messages. |
| `sourceDirectory` | `${project.build.sourceDirectory}` | Java source directory scanned by `gettext`. |
| `outputDirectory` | `${project.build.outputDirectory}` | Directory for generated bundles. |
| `extraSourceFiles` | not configured | Optional file set for extracting messages from additional source files. |
| `targetBundle` | required by `dist` | Fully qualified bundle name, such as `com.example.Messages`. |
| `outputFormat` | `class` | Bundle format: `class` or `properties`. |

Additional goal-specific options are available for encoding, extraction
keywords, sorting, fuzzy merging, Java bundle generation, and custom gettext
command paths.

## License

This project is licensed under the Apache License, Version 2.0. See
<https://www.apache.org/licenses/LICENSE-2.0>.
