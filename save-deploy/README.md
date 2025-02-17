# SAVE Cloud deployment configuration
## Components
SAVE Cloud contains the following microservices:
* backend: REST API for DB
* test-preprocessor: clones projects for test and discovers tests
* orchestrator: moderates distributed execution of tests, feeds new batches of tests to a set of agents
save-cloud uses MySQL as a database. Liquibase (via gradle plugin) is used for schema initialization and migration.

## Building
* Prerequisites: some components require additional system packages. See [save-agent](../save-agent/README.md) description for details.
  save-frontend requires node.js installation.
* To build the project and run all tests, execute `./gradlew build`.
* For deployment, all microservices are packaged as docker images with the version based on latest git tag and latest commit hash, if there are commits after tag.

Deployment is performed on server via docker swarm or locally via docker-compose. See detailed information below.

## Server deployment
* Server should run Linux and support docker swarm and gvisor runtime. Ideally, kernel 5.+ is required.
* [reverse-proxy.conf](reverse-proxy.conf) is a configuration for Nginx to act as a reverse proxy for save-cloud. It should be 
  copied into `/etc/nginx/sites-available`.
* Gvisor should be installed and runsc runtime should be available for docker. See [installation guide](https://gvisor.dev/docs/user_guide/install/) for details.
  A different runtime can be specified with `orchestrator.docker.runtime` property in orchestrator.
* Ensure that docker daemon is running and that docker is in swarm mode.
* Secrets should be added to the swarm as well as to `$HOME/secrets` file.
* If custom SSL certificates are used, they should be installed on the server and added into JDK's truststore inside images. See section below for details.
* Loki logging driver should be added to docker installation: [instruction](https://grafana.com/docs/loki/latest/clients/docker-driver/#installing)
* Pull new changes to the server and run `./gradlew -Psave.profile=prod deployDockerStack`.
  * If you wish to deploy save-cloud, that is not present in docker registry (e.g. to deploy from a branch), run `./gradlew -Psave.profile=prod buildAndDeployDockerStack` instead.
  * If you would like to use `docker-compose.override.yaml`, add `-PuseOverride=true` to the execution of tasks above.
    This file is configured to be read from `$HOME/configs`; you can use the one from the repository as an example.
* [`docker-compose.yaml.template`](../docker-compose.yaml.template) is configured so that all services use Loki for logging
  and configuration files from `~/configs`, which are copied from `save-deploy` during gradle build.

## Override configuration per server
If you wish to customize services configuration externally (i.e. leaving docker images intact), this is possible via additional properties files.
In [docker-compose.yaml.template](../docker-compose.yaml.template) all services have `/home/saveu/configs/<service name>` directory mounted. If it contains
`application.properties` file, it will override config from default `application.properties`.

## Running behind proxy
If save-cloud is running behind proxy, docker daemon should be configured to use proxy. See [docker docs](https://docs.docker.com/network/proxy/).
Additionally, use `/home/saveu/configs/orchestrator/application.properties` to add two flags to `apt-get`:
```properties
orchestrator.aptExtraFlags=-o Acquire::http::proxy="http://host.docker.internal:3128" -o Acquire::https::proxy="http://host.docker.internal:3128"
```
Proxy URLs will be resolved from inside the container.

## Custom SSL certificates
If custom SSL certificates are used, they should be installed on the server and added into JDK's truststore inside images.
One way of adding them into JDK is to mount them in docker-compose.yaml and then override default command:
```yaml
preprocessor:
  volume:
    - '/path/to/certs/cert.cer:/home/cnb/cert.cer'
  entrypoint: /bin/bash
  command: -c 'find /layers -name jre -type d -exec {}/bin/keytool -keystore {}/lib/security/cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias <cert-alias> -file /home/cnb/cert.cer \; && /cnb/process/web'
```

## Database
The service is designed to work with MySQL database. Migrations are applied with liquibase. They expect event scheduler to be enabled on the DB.

## Enabling api-gateway with external OAuth providers
In the file `/home/saveu/configs/gateway/application.properties` the following properties should be provided:
* `spring.security.oauth2.client.provider.<provider name>.issuer-uri`
* `spring.security.oauth2.client.registration.<provider name>.client-id`
* `spring.security.oauth2.client.registration.<provider name>.client-secret`
  
## Local deployment
Usually not the whole stack is required for development. Application logic is performed by save-backend, save-orchestrator and save-preprocessor, so most time you'll need those three.
* Ensure that docker daemon is running and docker-compose is installed.
  * If running on a system without Unix socket connection to the Docker Daemon (e.g. with Docker for Windows), docker daemon should have HTTP
    port enabled. Then, `docker-tcp` profile should be enabled for orchestrator.
* To make things easier, add line `save.profile=dev` to `gradle.properties`. This will make project version `SNAPSHOT` instead of timestamp-based suffix and allow caching of gradle tasks.
* Run `./gradlew deployLocal -Psave.profile=dev` to start the database and run three microservices (backend, preprocessor and orchestrator) with Docker Compose.

#### Note:
If a snapshot version of save-cli is required (i.e., the one which is not available on GitHub releases), then it can be
manually placed in `save-orchestrator/build/resources/main` before build, and it's version should be provided via `-PsaveCliVersion=...` when executing gradle.

## Ports allocation
| port | description                                |
|------|--------------------------------------------|
| 3306 | database (locally)                         |                     |
| 5800 | save-backend                               |
| 5100 | save-orchestrator                          |
| 5200 | save-test-preprocessor                     |
| 5300 | api-gateway                                |
| 9090 | prometheus                                 |
| 9091 | node_exporter                              |
| 9100 | grafana                                    |

## Secrets
* Liquibase is reading secrets from the secrets file located on the server in the `home` directory.
* PostProcessor is reading secrets for database connection from the docker secrets and fills the spring datasource. (DockerSecretsDatabaseProcessor class)
* api-gateway is a single external-facing component, hence its security is stricter. Actuator endpoints are protected with
basic HTTP security. Access can be further restricted by specifying `gateway.knownActuatorConsumers` in `application.properties`
(if this options is not specified, no check will be performed). To access this data from prometheus it should have access
to these credentials from docker secrets. Configuration file [prometheus.yml](./prometheus.yml) is configured to use username
`prometheus` and password from standard path for docker swarm secrets.

# Server configuration
## Nginx
Nginx is used as a reverse proxy, which allows access from external network to backend and some other services.
File `save-deploy/reverse-proxy.conf` should be copied to `/etc/nginx/sites-available`. Symlink should be created:
`sudo ln -s /etc/nginx/sites-available/reverse-proxy.conf /etc/nginx/sites-enabled/` (or to `/etc/nginx/conf.d` on some distributions).

# Adding a new service
Sometimes it's necessary to create a new service. These steps are required to seamlessly add it to deployment:
* Add it to [docker-compose.yaml.template](../docker-compose.yaml.template)
* Add it to task `depoyDockerStack` in [`DockerStackConfiguration.kt`](../buildSrc/src/main/kotlin/com/saveourtool/save/buildutils/DockerStackConfiguration.kt)
  so that config directory is created (if it's another Spring Boot service)
