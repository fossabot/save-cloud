# Contributing
1. Fork this repository to your own account
2. Make your changes and verify that tests pass (`./gradlew check`)
3. Run `diktat` static analyzer with `diktatFix` gradle task or `diktatCheck` task depending on what you want.
4. Run `detekt` with `detektAll` gradle task
5. After all tests and analyzers passing commit your work and push to a new branch on your fork
6. Submit a pull request
7. Participate in the code review process by responding to feedback

## Launching save-cloud with command line
1. Use `gradlew.bat startMysqlDb` on Windows and `./gradlew startMysqlDb` on other platforms for setting up database.
Make sure you have Docker installed and active.
2. Run backend.
It can be run either with `./gradlew save-backend:bootRun` or with Intellij Idea Ultimate plugin from the menu `Services`.
3. Run frontend. It can be run with `./gradlew save-frontend:run`.
You can enable hot reload by passing `--continuous` flag.
4. More specific instructions can be found in [save-deploy](save-deploy/README.md)

## Spring Intellij Idea Ultimate plugin
In order to make Spring Intellij Idea Ultimate plugin work properly, you need to set these active profiles in service's configuration:  

|         |  SaveApplication   | SaveGateway | SaveOrchestrator | SavePreprocessor | 
|:-------:|:------------------:|:-----------:|:----------------:|:----------------:|
|   Mac   | `mac, dev, secure` | `mac, dev`  |      `mac`       |      `mac`       |
| Windows |   `dev, secure`    |    `dev`    |       `-`        |       `-`        |
|  Linux  |   `dev, secure`    |    `dev`    |       `-`        |       `-`        |

### Mac M1 contributors
In file `save-cloud/build.gradle.kts` change languageVersion of `org.liquibase.gradle.LiquibaseTask` from 11 to 17
so there would be something like this:
```
tasks.withType<org.liquibase.gradle.LiquibaseTask>().configureEach {
    this.javaLauncher.set(project.extensions.getByType<JavaToolchainService>().launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
}
```

