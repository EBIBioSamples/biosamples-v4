# What Runs Where

This is an inventory of runnable programs and deployment entrypoints found in this repository. Fill in the `Runs where`, `Schedule / trigger`, `Owner`, and `Notes` columns with the operational details for your environments.

## Build Units

| Area | Module | Artifact | Packaging | Runnable here? | Runs where | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Root | `.` | `biosamples` | Maven parent POM | No |  | Parent build: `./mvnw -T 2C package`. |
| Webapps | `webapps/core` | `webapps-core` | Spring Boot WAR | Yes |  | Main BioSamples webapp/API. Local Compose maps `8081:8080` with context path `/biosamples`. |
| Webapps | `webapps/core-v2` | `webapps-core-v2` | Spring Boot JAR | Yes |  | BioSamples v2 API. Local Compose maps `8082:8080` with context path `/biosamples/v2`. |
| Agents | `agents/uploadworkers` | `agents-uploadworkers` | Spring Boot JAR | Yes |  | File upload worker consuming RabbitMQ and writing MongoDB state. |
| Pipelines | `pipelines/analytics` | `pipelines-analytics` | Spring Boot JAR | Yes |  | Analytics batch runner. |
| Pipelines | `pipelines/chain` | `pipelines-chain` | Spring Boot JAR | Yes |  | Helpdesk/action chain runner. |
| Pipelines | `pipelines/curation` | `pipelines-curation` | Spring Boot JAR | Yes |  | Curation pipeline. Local Compose has `biosamples-pipelines-curation`; CI builds a Docker target. |
| Pipelines | `pipelines/ncbi` | `pipelines-ncbi` | Spring Boot JAR | Yes |  | NCBI import pipeline. Local Compose has `biosamples-pipelines-ncbi`. |
| Pipelines | `pipelines/reindex` | `pipelines-reindex` | Spring Boot JAR | Yes |  | Reindex pipeline. Local Compose has `biosamples-pipelines-reindex`; CI and Helm jobs deploy it. |
| Pipelines | `pipelines/sample-post-release-action` | `pipelines-sample-post-release-action` | Spring Boot JAR | Yes |  | Post-release sample action pipeline. |
| Pipelines | `pipelines/sample-release` | `pipelines-sample-release` | Spring Boot JAR | Yes |  | Sample release pipeline. |
| Pipelines | `pipelines/sample-transformation-dtol` | `pipelines-sample-transformation-dtol` | Spring Boot JAR | Yes |  | DTOL sample transformation pipeline. |
| Pipelines | `pipelines/taxonimport` | `pipelines-taxonimport` | Spring Boot JAR | Yes |  | Taxonomy import pipeline. |
| Pipelines | `pipelines/zooma` | `pipelines-zooma` | Spring Boot JAR | Yes |  | Zooma annotation pipeline. Local Compose has `biosamples-pipelines-zooma`. |
| Pipelines | `pipelines/ncbi-ena-link` | `pipelines-ncbi-ena-link` | Spring Boot JAR | Source present, not in parent build |  | Module exists with an `ApplicationRunner`, but it is commented out in `pipelines/pom.xml`. |
| Integration | `integration` | `integration` | Spring Boot JAR | Yes |  | Integration test runner. Local scripts run it by `--phase`. |
| Libraries | `core`, `properties`, `logback`, `client/client`, `client/starter`, `pipelines/common` | Various | JAR | Mostly no |  | Shared libraries. `pipelines/common` includes `AmrDataLoaderService.main`, but it is not packaged as a normal Spring Boot app. |

## Spring Boot Entrypoints

| Program | Main class | Runner class | Usual command | Runs where | Schedule / trigger | Owner | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Webapp core | `webapps/core/src/main/java/uk/ac/ebi/biosamples/Application.java` | HTTP server | `java -jar webapps-core-<version>.war` |  |  |  | Compose service: `biosamples-webapps-core`. |
| Webapp core v2 | `webapps/core-v2/src/main/java/uk/ac/ebi/biosamples/Application.java` | HTTP server | `java -jar webapps-core-v2-<version>.jar` |  |  |  | Compose service: `biosamples-webapps-core-v2`. |
| Upload workers | `agents/uploadworkers/src/main/java/uk/ac/ebi/biosamples/Application.java` | `FileUploadMessageQueueRunner` | `java -jar agents-uploadworkers-<version>.jar` |  |  |  | Compose service: `biosamples-agents-upload-workers`. |
| Analytics pipeline | `pipelines/analytics/src/main/java/uk/ac/ebi/biosamples/Application.java` | `AnalyticsApplicationRunner` | `java -jar pipelines-analytics-<version>.jar` |  |  |  |  |
| Chain pipeline | `pipelines/chain/src/main/java/uk/ac/ebi/biosamples/Application.java` | `HelpdeskActionApplicationRunner` | `java -jar pipelines-chain-<version>.jar` |  |  |  |  |
| Curation pipeline | `pipelines/curation/src/main/java/uk/ac/ebi/biosamples/Application.java` | `CurationApplicationRunner` | `java -jar pipelines-curation-<version>.jar` |  |  |  | Compose and Kubernetes cron workflow reference this. |
| NCBI pipeline | `pipelines/ncbi/src/main/java/uk/ac/ebi/biosamples/Application.java` | `Ncbi` | `java -jar pipelines-ncbi-<version>.jar` |  |  |  | README documents `docker-compose up biosamples-pipelines-ncbi`. |
| NCBI ENA link pipeline | `pipelines/ncbi-ena-link/src/main/java/uk/ac/ebi/biosamples/Application.java` | `NcbiEnaLinkRunner` | `java -jar pipelines-ncbi-ena-link-<version>.jar` |  |  |  | Source present but not active in parent Maven build. |
| Reindex pipeline | `pipelines/reindex/src/main/java/uk/ac/ebi/biosamples/Application.java` | `ReindexRunner` | `java -jar pipelines-reindex-<version>.jar` |  |  |  | Compose service and Kubernetes job reference this. |
| Sample post-release action | `pipelines/sample-post-release-action/src/main/java/uk/ac/ebi/biosamples/Application.java` | `SamplePostReleaseActionApplicationRunner` | `java -jar pipelines-sample-post-release-action-<version>.jar` |  |  |  |  |
| Sample release pipeline | `pipelines/sample-release/src/main/java/uk/ac/ebi/biosamples/Application.java` | `SampleReleaseRunner` | `java -jar pipelines-sample-release-<version>.jar` |  |  |  |  |
| DTOL transformation pipeline | `pipelines/sample-transformation-dtol/src/main/java/uk/ac/ebi/biosamples/Application.java` | `TransformationApplicationRunner` | `java -jar pipelines-sample-transformation-dtol-<version>.jar` |  |  |  |  |
| Taxon import pipeline | `pipelines/taxonimport/src/main/java/uk/ac/ebi/biosamples/Application.java` | `TaxonImportApplicationRunner` | `java -jar pipelines-taxonimport-<version>.jar` |  |  |  |  |
| Zooma pipeline | `pipelines/zooma/src/main/java/uk/ac/ebi/biosamples/Application.java` | `ZoomaApplicationRunner` | `java -jar pipelines-zooma-<version>.jar` |  |  |  | Compose service: `biosamples-pipelines-zooma`. |
| Integration runner | `integration/src/main/java/uk/ac/ebi/biosamples/Application.java` | `AbstractIntegration` subclasses | `java -jar integration-<version>.jar --phase=<phase>` |  |  |  | Used by `docker-integration*.sh` and `docker-test.sh`. |

## Local Docker Compose Services

| Compose service | Type | Image / artifact | Local ports | Runs where | Notes |
| --- | --- | --- | --- | --- | --- |
| `biosamples-webapps-core` | Webapp | `biosamples:latest`, `webapps-core-5.3.15-SNAPSHOT.war` | `8081:8080`, `8000:8000` | Local dev | Depends on MongoDB, RabbitMQ, Solr, Neo4j, schema validator/store. |
| `biosamples-webapps-core-v2` | Webapp | `biosamples:latest`, `webapps-core-v2-5.3.15-SNAPSHOT.jar` | `8082:8080` | Local dev | Depends on MongoDB, RabbitMQ, schema validator/store. |
| `biosamples-agents-upload-workers` | Worker | `biosamples:latest`, `agents-uploadworkers-5.3.15-SNAPSHOT.jar` | None | Local dev | RabbitMQ/MongoDB worker. |
| `biosamples-pipelines-ncbi` | Pipeline | `biosamples:latest`, `pipelines-ncbi-5.3.15-SNAPSHOT.jar` | None | Local dev | Uses `BIOSAMPLES_CLIENT_URI=http://biosamples-webapps-core:8080/biosamples`. |
| `biosamples-pipelines-curation` | Pipeline | `biosamples:latest`, `pipelines-curation-5.3.15-SNAPSHOT.jar` | None | Local dev | Uses BioSamples webapp and OLS cache settings. |
| `biosamples-pipelines-zooma` | Pipeline | `biosamples:latest`, `pipelines-zooma-5.3.15-SNAPSHOT.jar` | None | Local dev | Uses BioSamples webapp, Zooma, and OLS cache settings. |
| `biosamples-pipelines-reindex` | Pipeline | `biosamples:latest`, `pipelines-reindex-5.3.15-SNAPSHOT.jar` | None | Local dev | Uses BioSamples webapp, MongoDB, RabbitMQ. |
| `biosamples-integration` | Test runner | `biosamples:latest`, `integration-5.3.15-SNAPSHOT.jar` | None | Local dev/CI-like local runs | Invoked with `--phase` by scripts. |
| `biosamples-search` | Search service | `biosamples-search:latest` | `8083:8080`, `9090:9090` | Local dev | Built from external checkout by `build-biosamples-search-image.sh`. |
| `json-schema-validator` | Support service | `quay.io/ebi-ait/biovalidator:2.0.1` | `3020:3020` | Local dev | Validation dependency. |
| `schema-store` | Support service | `biosamples/json-schema-store:1.1.0` | `8085:8085` | Local dev | Schema store dependency. |
| `mongo` | Support service | `mongo:4.4.22` | `27017:27017` | Local dev | Main local database. |
| `rabbitmq` | Support service | `rabbitmq:3.10.7-management-alpine` | `5672:5672`, `15672:15672` | Local dev | Messaging broker and management UI. |
| `solr` | Support service | `solr:8.11.2` | `8983:8983` | Local dev | Pre-creates `samples` core. |
| `neo4j` | Support service | `neo4j:4.0.3` | `7474:7474`, `7687:7687` | Local dev | Graph dependency. |
| `elastic` | Support service | `docker.elastic.co/elasticsearch/elasticsearch:${STACK_VERSION}` | `${ES_PORT}:9200` | Local dev | Search dependency for `biosamples-search`. |
| `elastic-setup` | Setup job | Elasticsearch image | None | Local dev | Sets Kibana system password after Elasticsearch starts. |

## Compose References That Need Verification

These are referenced by `docker-compose.yml` or scripts, but matching current source modules were not found in the parent Maven build.

| Reference | Where found | Expected artifact | Status | Runs where | Notes |
| --- | --- | --- | --- | --- | --- |
| `biosamples-agents-solr` | `docker-compose.yml`, `docker-agents.sh`, test scripts | `agents-solr-5.3.15-SNAPSHOT.jar` | No current `agents/solr` module found |  | Verify whether this is legacy or supplied externally. |
| `biosamples-pipelines-ena` | `docker-compose.yml` | `pipelines-ena-5.3.15-SNAPSHOT.jar` | `pipelines/ena` is commented out or absent |  | README mentions ENA import generally. |
| `biosamples-pipelines-accession` | `docker-compose.yml` | `pipelines-accession-5.3.15-SNAPSHOT.jar` | No current module found |  | Verify legacy/current ownership. |
| `biosamples-pipelines-copydown` | `docker-compose.yml`, `k8s/Dockerfile`, cron workflow | `pipelines-copydown-5.3.15-SNAPSHOT.jar` | No current module found |  | Kubernetes Dockerfile still has a `pipelines-copydown` target. |
| `biosamples-pipelines-export` | `docker-compose.yml` | `pipelines-export-5.3.15-SNAPSHOT.jar` | No current module found |  | Writes `/export/export.json.gzip` locally. |
| `pipelines-curami` | `k8s/Dockerfile`, cron workflow | `pipelines-curami-*.jar` | No current module found |  | Verify whether this moved to another repository. |
| `elasticsearch`, `logstash`, `kibana` | `docker-logging.sh` | Logging stack services | Not in current `docker-compose.yml` |  | Script appears stale unless another override file supplies these. |

## Kubernetes And CI Deployment Map

| Deployment surface | What it deploys | Environments / hosts found | How it is triggered | Runs where | Notes |
| --- | --- | --- | --- | --- | --- |
| GitLab CI build | Maven build for all parent modules | CI runner | `maven-package-all-apps` stage | GitLab CI | Uses `${CI_REGISTRY_IMAGE}/eclipse-temurin:17-jdk`. |
| GitLab CI Docker build | `webapp-core`, `webapp-core-v2`, `agents-uploadworkers`, `pipelines-reindex`, `pipelines-curation` | CI registry | `build_and_push_docker_images` stage | GitLab CI | Builds `k8s/Dockerfile` targets and pushes tagged/latest images. |
| Helm app deploy | Webapp core, webapp core v2, upload workers | `primary_dev`, `primary_prod`, `fallback_prod`; RabbitMQ hosts `wp-np2-40.ebi.ac.uk`, `wp-p1m-42.ebi.ac.uk`, `wp-p2m-42.ebi.ac.uk` | Manual GitLab jobs `deploy_k8s_*` | Kubernetes | Uses `k8s/helm`. |
| Helm job deploy | Reindex pipeline job | `primary_dev`, `primary_prod`, `fallback_prod`; same RabbitMQ host pattern | Manual GitLab jobs `deploy_pipeline_k8s_*` | Kubernetes | Uses `k8s/jobs`. |
| Argo workflow deploy | Curation, curami, copydown pipeline DAG | `primary_dev`; RabbitMQ host `wp-np2-40.ebi.ac.uk` | Manual GitLab job `deploy_cronjobs_k8s_primary_dev` | Kubernetes / Argo | Uses `k8s/cronjobs`; no schedule was found in chart values, so confirm external trigger/schedule. |

## Environment Runtime Matrix

Use this table to record the actual runtime location for each service in dev and production. `Configured in repo` means this repository has CI, Helm, Compose, or values-file wiring for that environment; it does not prove the service is currently running.

| Component | Type | Runs in dev? | Dev location / host | Runs in prod? | Prod location / host | Prod only? | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `webapps-core` | Webapp/API | Yes | K8s primary cluster `hh-wp-webadmin-69`; namespace `biosamples-dev` | Yes | K8s primary cluster `hh-wp-webadmin-69`; fallback cluster `hx-wp-webadmin-74`; namespace `biosamples-prod` | No | Main `/biosamples` webapp/API. |
| `webapps-core-v2` | Webapp/API | Yes | K8s primary cluster `hh-wp-webadmin-69`; namespace `biosamples-dev` | Yes | K8s primary cluster `hh-wp-webadmin-69`; fallback cluster `hx-wp-webadmin-74`; namespace `biosamples-prod` | No | `/biosamples/v2` API. |
| `agents-uploadworkers` | Worker | Yes | K8s primary cluster `hh-wp-webadmin-69`; namespace `biosamples-dev` | Yes | K8s primary cluster `hh-wp-webadmin-69`; fallback cluster `hx-wp-webadmin-74`; namespace `biosamples-prod` | No | File upload queue worker. |
| `biosamples-search` | Search webapp/service | Yes | K8s primary cluster `hh-wp-webadmin-69`; namespace `biosamples-dev`; service `biosamples-search-helm:9090` | Yes | K8s primary cluster `hh-wp-webadmin-69`; fallback cluster `hx-wp-webadmin-74`; namespace `biosamples-prod`; service `biosamples-search-helm:9090` | No | External image/service relative to this repo. Local Compose maps `8083:8080`, `9090:9090`. |
| MongoDB | Database | Yes | Details in `biosamples-internal` GitLab repo | Yes | Details in `biosamples-internal` GitLab repo | No | Source: https://gitlab.ebi.ac.uk/biosamples/biosamples-internal. Used by webapps, upload workers, reindex, schema store. |
| Elasticsearch | Search datastore | Yes, configured locally; Kubernetes location TBD | Local Compose `elastic:${ES_PORT}->9200`; K8 host TBD | TBD | Host TBD | TBD | Required by `biosamples-search`. |
| RabbitMQ | Message broker | Yes, configured in repo | `wp-np2-40.ebi.ac.uk` | Yes, configured in repo | Primary `wp-p1m-42.ebi.ac.uk`; fallback `wp-p2m-42.ebi.ac.uk` | No | Used by webapps, upload workers, pipelines. |
| `json-schema-validator` / BioValidator | Schema validation service | Yes, configured in repo | Local Compose `3020`; K8 URL `http://biovalidator-service:3020/biosamples/biovalidator/validate` | Yes, configured in repo | Host/URL TBD | No | Webapps use `biosamples.schema.validator.url`. |
| `schema-store` | Schema store service | Yes, configured in repo | Local Compose `8085`; K8 URL TBD | Yes, configured in repo | `https://www.ebi.ac.uk/biosamples/schema-store` or environment-specific URL TBD | No | Webapps use `biosamples.schema.store.url`. |
| Solr | Search datastore, legacy/local | Yes, local Compose | Local Compose `8983`; K8 dev TBD | TBD | Host TBD | TBD | Compose dependency for `webapps-core`; Kubernetes config points `biosamples-search` instead. |
| Neo4j | Graph database | Yes, local Compose | Local Compose `7474`, `7687`; K8 dev TBD | TBD | Host TBD | TBD | Compose dependency for graph features. |
| `pipelines-reindex` | Pipeline/job | No current dev runtime provided | Argo/K8s migration is work in progress | Yes | VM `wp-p2m-40.ebi.ac.uk` | Yes | Current runtime is VM. `k8s/jobs` exists but Argo/K8s pipeline work is not complete. |
| `pipelines-curation` | Pipeline/workflow | No current dev runtime provided | Argo/K8s migration is work in progress | Yes | VM `wp-p2m-40.ebi.ac.uk` | Yes | Current runtime is VM. `k8s/cronjobs` has work-in-progress Argo workflow wiring. |
| `pipelines-curami` | Pipeline/workflow | No current dev runtime provided | Argo/K8s migration is work in progress | Yes | VM `wp-p2m-40.ebi.ac.uk` | Yes | Referenced by `k8s/cronjobs` and `k8s/Dockerfile`, but source module not found here. |
| `pipelines-copydown` | Pipeline/workflow | No current dev runtime provided | Argo/K8s migration is work in progress | Yes | VM `wp-p2m-40.ebi.ac.uk` | Yes | Referenced by `k8s/cronjobs` and `k8s/Dockerfile`, but source module not found here. |
| `pipelines-ncbi` | Pipeline | No current dev runtime provided | Local Compose only | Yes | VM `wp-p2m-40.ebi.ac.uk` | Yes | Buildable module exists. Current runtime is VM, not K8s. |
| `pipelines-zooma` | Pipeline | No current dev runtime provided | Local Compose only | Yes | VM `wp-p2m-40.ebi.ac.uk` | Yes | Buildable module exists. Current runtime is VM, not K8s. |
| `pipelines-analytics` | Pipeline | No current dev runtime provided |  | Yes | VM `wp-p2m-40.ebi.ac.uk` | Yes | Buildable module exists. Current runtime is VM, not K8s. |
| `pipelines-chain` | Pipeline | No current dev runtime provided |  | Yes | VM `wp-p2m-40.ebi.ac.uk` | Yes | Buildable module exists. Current runtime is VM, not K8s. |
| `pipelines-sample-release` | Pipeline | No current dev runtime provided |  | Yes | VM `wp-p2m-40.ebi.ac.uk` | Yes | Buildable module exists. Current runtime is VM, not K8s. |
| `pipelines-sample-post-release-action` | Pipeline | No current dev runtime provided |  | Yes | VM `wp-p2m-40.ebi.ac.uk` | Yes | Buildable module exists. Current runtime is VM, not K8s. |
| `pipelines-sample-transformation-dtol` | Pipeline | No current dev runtime provided |  | Yes | VM `wp-p2m-40.ebi.ac.uk` | Yes | Buildable module exists. Current runtime is VM, not K8s. |
| `pipelines-taxonimport` | Pipeline | No current dev runtime provided |  | Yes | VM `wp-p2m-40.ebi.ac.uk` | Yes | Buildable module exists. Current runtime is VM, not K8s. |
| `pipelines-ncbi-ena-link` | Pipeline | No, not in parent build |  | TBD | Host/cluster TBD | TBD | Source exists but module is commented out in `pipelines/pom.xml`. |
| `integration` | Integration test runner | Local only in repo | Local Compose/scripts | No |  | No | Used by local integration scripts, not an app deployment. |

## Helper Scripts

| Script | What it runs | Runs where | Notes |
| --- | --- | --- | --- |
| `docker-webapp.sh` | Builds with Maven, starts local dependencies, creates Elasticsearch index, starts `biosamples-search`, `biosamples-webapps-core`, and `biosamples-webapps-core-v2` | Local dev | Supports `--clean`; uses `ci_settings.xml`. |
| `docker-debug.sh` | Builds with Maven, starts Solr/RabbitMQ/MongoDB, configures Solr/MongoDB, starts webapps with debug override | Local dev | Expects `docker-compose.debug.override.yml` when used. |
| `docker-integration.sh` | Starts webapp stack, upload workers, then runs integration phases | Local dev | Runs phases `01 02 03 04 05 06 07 08 09`. |
| `docker-integration.big.sh` | Starts webapp stack and both agents, then runs larger integration phases | Local dev | References `biosamples-agents-solr`, which needs verification. |
| `docker-test.sh` | Runs selected integration phases against Compose services | Local dev | References `biosamples-agents-solr`, which needs verification. |
| `docker-agents.sh` | Runs `biosamples-agents-solr` | Local dev | Needs verification because no current `agents-solr` module was found. |
| `docker-logging.sh` | Starts logging stack | Local dev | References services not present in current `docker-compose.yml`. |
| `build-biosamples-search-image.sh` | Builds external `biosamples-search` Docker image and starts local Elasticsearch | Local dev | Default external path is `/mnt/c/users/dgupta/biosamples-peripheral-services/biosamples-search`; override with `REPO_DIR`. |
| `http-status-check` | Polls a URL until HTTP 200 or timeout | Local dev / scripts | Generic startup check helper. |
| `update-versions.sh` | Runs Maven versions plugin and updates Docker/script version strings | Release maintenance | Creates/removes `*.versionsBackup` files. |

## Fill-In Checklist

| Item | Value           |
| --- |-----------------|
| Dev Kubernetes namespace(s) | biosamples-dev on `hh-wp-webadmin-69` |
| Production Kubernetes namespace(s) | biosamples-prod |
| Fallback production namespace(s) | biosamples-prod on `hx-wp-webadmin-74` |
| Public URL for core API |                 |
| Public URL for v2 API |                 |
| Internal URL for `biosamples-search` |                 |
| RabbitMQ host(s) by environment |                 |
| MongoDB host/secret names by environment | See `biosamples-internal`: https://gitlab.ebi.ac.uk/biosamples/biosamples-internal |
| Pipeline schedule source | Current runtime: VM `wp-p2m-40.ebi.ac.uk`; Argo/K8s migration is work in progress |
| On-call / owning team |                 |
| Dashboard links |                 |
| Runbook links |                 |

## K8 Clusters

| Cluster | Role | Webadmin host | Namespace(s) | Notes |
| --- | --- | --- | --- | --- |
| HL2 | Primary | `hh-wp-webadmin-69` | `biosamples-dev`, `biosamples-prod` | Dev runs only here. Prod also runs here as the primary cluster. |
| HL | Fallback | `hx-wp-webadmin-74` | `biosamples-prod` | Prod fallback cluster. Dev does not run here. |
