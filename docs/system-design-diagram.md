# BioSamples v4 System Design Diagram

This diagram is based on the Maven modules in `pom.xml`, the local deployment topology in
`docker-compose.yml`, and the shared infrastructure wiring in `core`.

## Runtime Architecture

```mermaid
flowchart LR
  users[Users and browsers]
  api_clients[API clients and downstream services]
  submitters[Submitters with ENA WEBIN accounts]
  external_sources[External data sources<br/>ENA, NCBI, taxonomy, Zooma, OLS]

  subgraph app[BioSamples application layer]
    web_v1[webapps-core<br/>Spring Boot WAR<br/>/biosamples]
    web_v2[webapps-core-v2<br/>Spring Boot JAR<br/>/biosamples/v2]
    client_lib[client + starter<br/>Java client library]
    pipelines[Pipelines<br/>import, curation, release,<br/>reindex, analytics, export]
    upload_workers[agents-uploadworkers<br/>file upload processing]
    solr_agent[agents-solr<br/>indexing worker<br/>Docker runtime artifact]
    search_app[biosamples-search<br/>Elasticsearch-backed search service]
    integration[integration<br/>graph and API integration jobs]
  end

  subgraph core[Shared core module]
    domain[Domain model, conversion,<br/>validation, security, services]
    mongo_access[Mongo repositories]
    solr_access[Solr repository]
    neo_access[Neo4j graph repositories]
    messaging[Messaging config and helpers]
  end

  subgraph async[RabbitMQ]
    upload_q[biosamples.uploaded.files]
    index_q[biosamples.indexing.es]
    reindex_q[biosamples.reindexing.es]
  end

  subgraph stores[Persistence and search]
    mongo[(MongoDB<br/>sample, curation, upload,<br/>pipeline state)]
    solr[(Solr<br/>legacy sample search)]
    neo4j[(Neo4j<br/>sample graph)]
    elastic[(Elasticsearch<br/>search backend)]
  end

  subgraph validation[Schema services]
    schema_store[JSON schema store]
    validator[BioValidator<br/>JSON schema validator]
  end

  subgraph ops[Operational concerns]
    logs[(Shared logs volume)]
    metrics[Micrometer / Stackdriver]
    k8s[Kubernetes Helm charts<br/>deployments, jobs, cronjobs]
  end

  users --> web_v1
  users --> web_v2
  api_clients --> client_lib
  client_lib --> web_v1
  client_lib --> web_v2
  submitters --> web_v1
  submitters --> web_v2

  external_sources --> pipelines
  pipelines --> web_v1
  pipelines --> mongo
  pipelines --> async

  web_v1 --> domain
  web_v2 --> domain
  upload_workers --> domain
  solr_agent --> domain
  integration --> domain

  domain --> mongo_access
  domain --> solr_access
  domain --> neo_access
  domain --> messaging

  mongo_access --> mongo
  solr_access --> solr
  neo_access --> neo4j
  messaging --> async

  web_v1 --> schema_store
  web_v2 --> schema_store
  schema_store --> mongo
  schema_store --> validator

  web_v1 --> upload_q
  upload_q --> upload_workers
  upload_workers --> mongo

  web_v1 --> index_q
  web_v1 --> reindex_q
  index_q --> solr_agent
  reindex_q --> solr_agent
  index_q --> search_app
  reindex_q --> search_app
  solr_agent --> solr
  search_app --> elastic

  web_v1 --> search_app
  web_v1 --> solr
  web_v1 --> mongo
  web_v1 --> neo4j
  web_v2 --> mongo
  web_v2 --> neo4j

  web_v1 --> metrics
  web_v2 --> metrics
  web_v1 --> logs
  web_v2 --> logs
  pipelines --> logs
  upload_workers --> logs
  integration --> logs
  k8s -. deploys .-> web_v1
  k8s -. deploys .-> web_v2
  k8s -. deploys .-> pipelines
```

## Primary Write And Index Flow

```mermaid
sequenceDiagram
  autonumber
  actor Submitter
  participant API as webapps-core or core-v2 API
  participant Validation as schema-store / BioValidator
  participant Mongo as MongoDB
  participant Rabbit as RabbitMQ
  participant UploadWorker as agents-uploadworkers
  participant IndexWorker as indexing/search workers
  participant Solr as Solr
  participant Search as biosamples-search
  participant Elastic as Elasticsearch
  participant Neo4j as Neo4j

  Submitter->>API: Submit sample metadata or upload file
  API->>Validation: Validate schema when required
  Validation-->>API: Validation result
  API->>Mongo: Persist sample, curation, upload, and state records
  API->>Rabbit: Publish upload or indexing event
  Rabbit-->>UploadWorker: Consume uploaded-file message
  UploadWorker->>Mongo: Process file-backed submission state
  Rabbit-->>IndexWorker: Consume indexing or reindexing message
  IndexWorker->>Solr: Update legacy search index
  IndexWorker->>Search: Update Elasticsearch-backed search path
  Search->>Elastic: Store or query indexed sample documents
  API->>Search: Delegate Elasticsearch-backed search
  API->>Neo4j: Read graph relationships for graph search views
  API-->>Submitter: HAL/JSON/XML/HTML response
```

## Maven Module Map

```mermaid
flowchart TB
  root[biosamples parent POM]

  root --> core_mod[core<br/>domain, persistence adapters,<br/>messaging, validation, security]
  root --> webapps_mod[webapps]
  root --> pipelines_mod[pipelines]
  root --> agents_mod[agents]
  root --> client_mod[client]
  root --> integration_mod[integration]
  root --> props_mod[properties]
  root --> logback_mod[logback]

  webapps_mod --> web_core[webapps-core<br/>legacy/main API and HTML UI]
  webapps_mod --> web_core_v2[webapps-core-v2<br/>v2 API]

  pipelines_mod --> common[common]
  pipelines_mod --> analytics[analytics]
  pipelines_mod --> curation[curation]
  pipelines_mod --> zooma[zooma]
  pipelines_mod --> dtol[sample-transformation-dtol]
  pipelines_mod --> ncbi[ncbi]
  pipelines_mod --> sample_release[sample-release]
  pipelines_mod --> taxonimport[taxonimport]
  pipelines_mod --> reindex[reindex]
  pipelines_mod --> chain[chain]
  pipelines_mod --> post_release[sample-post-release-action]

  agents_mod --> uploadworkers[uploadworkers]

  client_mod --> client_core[client/client<br/>BioSamplesClient and services]
  client_mod --> starter[client/starter<br/>Spring Boot auto-configuration]

  web_core --> core_mod
  web_core_v2 --> core_mod
  uploadworkers --> core_mod
  pipelines_mod --> core_mod
  integration_mod --> core_mod
  client_core -. runtime HTTP .-> web_core
  starter --> client_core
```

## Notes

- `core` is the shared application foundation. It contains Mongo, Solr, and Neo4j adapters, validation clients, messaging constants/configuration, security services, and domain conversion logic.
- `webapps-core` is the main deployed API/UI at `/biosamples`; `webapps-core-v2` exposes the newer `/biosamples/v2` API surface.
- Pipelines are batch/import/transformation jobs. In local Docker they call the main API through `BIOSAMPLES_CLIENT_URI`, while some pipeline modules also use shared persistence helpers.
- RabbitMQ is used for uploaded-file processing and indexing/reindexing queues:
  `biosamples.uploaded.files`, `biosamples.indexing.es`, and `biosamples.reindexing.es`.
- `docker-compose.yml` includes `biosamples-agents-solr`, but the current Maven `agents` aggregate only lists `uploadworkers`. Treat `agents-solr` as a runtime artifact or legacy/external module unless the build configuration is restored.
- Kubernetes assets under `k8s/` describe deployment, job, and cronjob packaging for non-local environments.
