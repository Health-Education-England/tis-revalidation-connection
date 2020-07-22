# TIS Revalidation Connection

## About
This is a service to manage revalidation connections with the following
technology:

 - Java 11
 - Spring Boot
 - Gradle
 - JUnit 5

Boilerplate code is to be generated with:
 - Lombok
 - MapStruct

Code quality checking and enforcement is done with the following tools:
 - EditorConfig
 - Checkstyle
 - JaCoCo
 - SonarQube

Error and exception logging is done using Sentry.

## TODO
 - Set up Sentry project.
 - Provide `SENTRY_DSN` and `SENTRY_ENVIRONMENT` as environmental variables
   during deployment.
 - Add repository to SonarCloud.
 - Add SonarCloud API key to repository secrets.
 - Add repository to Dependabot.

## Workflow
The `CI/CD Workflow` is triggered on push to any branch.

![CI/CD workflow](.github/workflows/ci-cd-workflow.svg "CI/CD Workflow")

## Versioning
This project uses [Semantic Versioning](semver.org).

## License
This project is license under [The MIT License (MIT)](LICENSE).

[task-definition]: .aws/task-definition.json
