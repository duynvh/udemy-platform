# Task

T01 — Maven parent and wrapper. Task file: [tasks/platform/T01-maven-parent.md](../../../tasks/platform/T01-maven-parent.md). Spec: [SPEC-platform.md](../../../spec/SPEC-platform.md) (Tech Stack, Commands, Code Style, Testing Strategy).

# What I changed

| File | What it does |
|---|---|
| `backend/pom.xml` | Parent for all Java modules. Inherits `spring-boot-starter-parent` 4.0.10. Imports the Spring Cloud 2025.1.5, Spring Modulith 2.0.10, and Testcontainers 2.0.5 BOMs. Configures Spotless, Surefire (`*Test`), Failsafe (`*IT`), and the JaCoCo 80% line gate. |
| `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties` | Maven Wrapper (script-only type). Downloads Maven 3.9.16 on first run. No binary jar in the repository. |
| `backend/.mvn/settings.xml` | Project-only Maven settings. Uses Maven Central (no mirror) and a separate local repository `~/.m2/repository-marketplace`. |
| `backend/.mvn/maven.config` | Makes every `mvnw` run use the project settings file as both user and global settings. |
| `.gitignore`, `.gitattributes`, `.editorconfig` (root) | Ignore build output and `infra/.env`. Force LF for `*.sh` and `mvnw`, CRLF for `*.cmd`. Same indentation in every editor. |

# The real problem

Several Java applications (`marketplace-api`, `bff-gateway`, `media-worker`) must use the same library versions. Without one parent, each application chooses its own versions. Then Spring Boot, Spring Cloud, and Spring Modulith versions do not match, and the build fails at runtime, not at compile time.

Two machine problems also appeared. First, the default `java` on this machine is Java 11 and the installed Maven is 3.6.1, but the project needs Java 25 and Maven 3.9. Second, the global Maven setup sends every download to a company Nexus server that is not always reachable. Without a fix, the build depends on the developer's private configuration.

# Why this solution

- **`spring-boot-starter-parent` as parent:** it gives tested defaults for the compiler, Surefire, Failsafe, and resource filtering. We only add what is special to this project.
- **BOM imports for the other release trains:** one property per train (`spring-cloud.version`, `spring-modulith.version`). Child modules declare dependencies without versions.
- **Spring Boot 4.0.x, not the newest 4.2.0:** the spec pins 4.0.x. Spring Cloud 2025.1 and Spring Modulith 2.0 are built for Boot 4.0. Mixing trains is the most common cause of startup errors.
- **Maven Wrapper:** every developer and CI run the same Maven version (3.9.16), independent of the installed Maven.
- **Project settings in `.mvn/maven.config`:** the build is reproducible on any machine. The developer's company settings are not changed; they are only ignored inside this project.
- **Separate local repository (`repository-marketplace`):** the project does not mix artifacts with the company cache.
- **Maven root in `backend/`:** backend and frontend are separate. The root folder holds only cross-cutting things (infra, docs, spec, tasks).

# Production shape

- CI runs `./mvnw` from `backend/`. It needs no Maven installation, only a JDK 25 (Temurin in GitHub Actions).
- In a company network, replace Maven Central in `backend/.mvn/settings.xml` with an artifact proxy (Nexus or Artifactory) that the CI can reach. Change one file; no developer machine changes.
- The JaCoCo gate fails `verify` in CI when domain or application coverage drops below 80%.
- Dependency updates come from Renovate (T20/T21 area). It updates the version properties in this one file.

# Other possible approaches

1. Gradle with Kotlin DSL and a version catalog.
2. No parent: each application has its own `pom.xml` with its own versions.
3. Our own parent without `spring-boot-starter-parent`, importing `spring-boot-dependencies` as a BOM.
4. Change the developer's global `~/.m2/settings.xml` or the Maven installation to use Maven Central.

# Why I did not choose those alternatives

1. Gradle: the user chose Maven. Maven is also simpler to read for a team that knows it.
2. No parent: versions drift between applications; three files to update for every upgrade.
3. Own parent with BOM only: valid when the company already has a corporate parent. Here it only adds plugin configuration we would copy from Spring Boot.
4. Global change: it breaks the developer's other (company) projects and does not help CI or other developers.

# Key concepts to learn

- **Parent POM:** a POM whose configuration (properties, plugins, dependency management) child modules inherit.
- **BOM (Bill of Materials):** a POM with only `dependencyManagement`. `scope=import` copies its versions into your project.
- **`dependencyManagement` vs `dependencies`:** the first only fixes versions; the second actually adds a library.
- **Release train:** a set of libraries tested together (Spring Cloud 2025.1 = for Spring Boot 4.0).
- **Maven Wrapper:** a script that downloads a fixed Maven version, so the build does not depend on the installed Maven.
- **`.mvn/maven.config`:** command-line options that Maven adds to every run in this project.
- **Surefire vs Failsafe:** Surefire runs unit tests in the `test` phase; Failsafe runs integration tests in `integration-test` and fails the build in `verify`, after cleanup.
- **JaCoCo check:** reads coverage data and fails the build if a rule (here 80% lines) is not met.

# Common mistakes

- Mixing release trains (for example Spring Cloud 2026.0 with Spring Boot 4.0). Check the compatibility table before an upgrade.
- Putting libraries in `<dependencies>` of the parent: every child then gets them, also the ones that do not need them.
- Using plugin versions that need a newer Maven. Here `maven-wrapper-plugin` 3.3.4 needed Maven ≥ 3.6.3; the installed 3.6.1 failed, so we used 3.2.0 only to generate the wrapper.
- Committing `maven-wrapper.jar` without a reason. The script-only wrapper avoids a binary file in Git.
- Shell scripts with CRLF line endings: they fail in Linux containers with `/bin/sh^M: bad interpreter`. `.gitattributes` prevents it.
- A coverage gate on all packages: adapters and configuration classes then need fake tests only to reach the number.

# Small example

```powershell
# Run in backend/ with JDK 25 (the default java on this machine is 11)
$env:JAVA_HOME = "C:\projects\secutix2\tools\jdk\jdk-25.0.4"
.\mvnw.cmd -v          # Apache Maven 3.9.16, Java version: 25.0.4
.\mvnw.cmd spotless:check
.\mvnw.cmd verify      # BUILD SUCCESS; JaCoCo skipped (no code yet)
```

A child module (T02) then declares dependencies without versions:

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-core</artifactId>
</dependency>
```

# How to think about this next time

- Ask first: "Which versions must move together?" Put each group behind one property or one BOM.
- Ask: "Does the build depend on anything private to my machine?" (default JDK, installed Maven, global settings). Pin it in the repository (wrapper, `.mvn/`).
- Ask: "Which tool version does this plugin need?" before you use the newest plugin with an old tool.
- Keep the parent small: versions and plugin rules only. Real dependencies belong to the module that uses them.
