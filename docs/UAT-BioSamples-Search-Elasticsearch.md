# User Acceptance Testing (UAT): BioSamples Search – Elasticsearch Migration

**Document version:** 1.0  
**Last updated:** February 2025  
**Scope:** BioSamples search use cases for the move from Solr to Elasticsearch (biosamples-search service).

---

## 1. Purpose and scope

### 1.1 Objective
This UAT defines test scenarios to verify that BioSamples search behaviour is correct and acceptable after the migration to **Elasticsearch** via the **biosamples-search** service. It covers the public search API, filters, pagination, and integration with the BioSamples web application.

### 1.2 In scope
- **Sample search API:** `GET /biosamples/samples` (HAL/JSON, cursor and page-based)
- **Accessions API:** `GET /biosamples/accessions` (when backed by the same search index; confirm in your deployment whether accessions use biosamples-search/Elasticsearch or another backend)
- **Search behaviour:** free text, filters, sort, pagination (page and cursor)
- **biosamples-search:** gRPC integration; host/port configuration
- **Documentation alignment:** behaviour matches [BioSamples Search Guide](https://www.ebi.ac.uk/biosamples/docs/guides/search) and [Filters reference](https://www.ebi.ac.uk/biosamples/docs/references/filters)

### 1.3 Out of scope (for this UAT)
- Submission, curation, and validation APIs
- Graph search
- Performance/load testing (can be a separate UAT)
- Internal indexing pipelines (covered elsewhere)

### 1.4 References
- **biosamples-search:** [EBIBioSamples/biosamples-search](https://github.com/EBIBioSamples/biosamples-search) – Elasticsearch-backed search service (gRPC on port 9090 by default)
- **BioSamples API:** `SamplesRestController`, `AccessionsGetController`, `ElasticSearchService`, `SamplePageService`
- **Docs:** `guides_search.adoc`, `ref_filters.adoc`, `ref_api_search.adoc`

---

## 2. Prerequisites and environment

| Item | Requirement |
|------|-------------|
| **Environment** | UAT environment with BioSamples webapp + biosamples-search + Elasticsearch (e.g. `docker-compose up` or k8s) |
| **Base URL** | e.g. `https://www.ebi.ac.uk/biosamples` (prod) or `http://localhost:8081/biosamples` (local) |
| **Auth (optional)** | WEBIN token for tests that require private/draft sample visibility |
| **Data** | Representative set of public (and optionally private) samples with attributes, relationships, external refs, dates |

**Configuration (BioSamples API → biosamples-search):**
- `biosamples.search.host` (e.g. `biosamples-search` or `biosamples-search-helm`)
- `biosamples.search.port` (default `9090`)

---

## 3. Search API summary

### 3.1 Sample search – `GET /biosamples/samples`

| Parameter | Required | Description |
|-----------|----------|-------------|
| `text` | No | Free-text search (phrases, boolean, wildcards, range per docs) |
| `filter` | No | Repeatable; filter type format `<type>:<field>:<value>` or existence checks |
| `cursor` | No* | Cursor for next page; default `*` when neither `cursor` nor `page` provided |
| `page` | No | Page number (0-based); use with `size`; max page 500 |
| `size` | No | Page size (default 20; max 200 for page, 1000 for cursor) |
| `sort` | No | Sort spec(s) (e.g. `update,desc`, `accession,asc`) |
| `applyCurations` | No | Default `true`; whether to apply curations to returned samples |

*When both `cursor` and `page` are null, API uses cursor-based pagination with `cursor=*`.

**Response:** HAL+JSON collection of sample resources; cursor responses include `next` link with new cursor.

### 3.2 Accessions – `GET /biosamples/accessions`

| Parameter | Required | Description |
|-----------|----------|-------------|
| `text` | No | Same as sample search |
| `filter` | No | Same filter syntax as sample search |
| `page` | No | Default 0 |
| `size` | No | Default 100 |

**Response:** JSON collection of accessions with pagination metadata and links.

### 3.3 Filter types (from BioSamples documentation)

| Type | Serialization | Example |
|------|----------------|---------|
| Attribute | `attr` | `attr:organism:Homo sapiens`, `attr:project` (attribute exists) |
| Accession | `acc` | `acc:SAMEA341514`, `acc:SAMN.*` (NCBI) |
| Date range | `dt` | `dt:release:from=2014-01-01until=2015-01-01`, `dt:update:from=2018-01-01` |
| Relationship | `rel` | `rel:derived+from:SAMEA7992418` |
| Inverse relationship | `rrel` | `rrel:has+member:SAMEG316651` |
| External reference | `extd` | `extd:ENA:SRS359918`, `extd:ArrayExpress:E-MTAB-3732` |
| Name | `name` | `name:Generic+sample+from+Glycine+max` |
| Domain | `dom` | `dom:self.<domain_hash>` |
| WEBIN ID | `webinId` | For authenticated user’s submissions |
| Structured data | `strd` | Filter by structured data type |

---

## 4. UAT test scenarios

**Pass criteria:** Each scenario must complete with the expected result and no regression vs documented behaviour. Any deviation must be documented and accepted or raised as a defect.

---

### 4.1 Basic search and discovery

| ID | Scenario | Steps | Expected result | Pass / Fail |
|----|----------|--------|------------------|-------------|
| **S1.1** | Empty search returns samples | `GET /biosamples/samples` (no `text`, no `filter`) | 200; list of samples (cursor or first page); total count ≥ 0 | |
| **S1.2** | Empty search with cursor | `GET /biosamples/samples?cursor=*&size=10` | 200; up to 10 samples; `next` link present if more results | |
| **S1.3** | Free-text search | `GET /biosamples/samples?text=human` | 200; samples relevant to “human” (e.g. organism/attributes); results consistent with docs | |
| **S1.4** | Phrase search | `GET /biosamples/samples?text="breast cancer"` | 200; samples matching phrase “breast cancer” as documented | |
| **S1.5** | Boolean query | `GET /biosamples/samples?text=NOT Leukemia AND (mouse OR human)` | 200; results match boolean logic per Search guide | |
| **S1.6** | Wildcard (mid/end of word) | `GET /biosamples/samples?text=leuk*mia` | 200; matches e.g. leukemia, leukaemia; no wildcard at start of word required by docs | |
| **S1.7** | Date range in text | `GET /biosamples/samples?text=updatedate:[2014-04-01 TO 2014-04-04]` | 200; samples updated in that range (if supported by backend) | |

---

### 4.2 Attribute and accession filters

| ID | Scenario | Steps | Expected result | Pass / Fail |
|----|----------|--------|------------------|-------------|
| **S2.1** | Attribute filter (type only) | `GET /biosamples/samples?filter=attr:project` | 200; only samples that have attribute “project” | |
| **S2.2** | Attribute filter (type + value) | `GET /biosamples/samples?filter=attr:organism:Homo sapiens` | 200; samples with organism = Homo sapiens (case-sensitive per docs) | |
| **S2.3** | Multiple attribute values (OR) | `GET /biosamples/samples?filter=attr:project:FAANG&filter=attr:project:HipSci` | 200; samples in FAANG or HipSci (per filters doc) | |
| **S2.4** | Accession exact | `GET /biosamples/samples?filter=acc:SAMEA341514` | 200; at most one sample; accession SAMEA341514 if exists | |
| **S2.5** | Accession pattern (NCBI) | `GET /biosamples/samples?filter=acc:SAMN.*` | 200; only samples with accessions matching SAMN.* | |
| **S2.6** | Accessions endpoint with filter | `GET /biosamples/accessions?filter=attr:organism:homo%20sapiens&size=100` | 200; list of accessions; same filter semantics as samples | |

---

### 4.3 Date, relationship, and external reference filters

| ID | Scenario | Steps | Expected result | Pass / Fail |
|----|----------|--------|------------------|-------------|
| **S3.1** | Date range – release | `GET /biosamples/samples?filter=dt:release:from=2014-01-01T00:00:00until=2015-01-01` | 200; samples released in range (ISO8601/UTC) | |
| **S3.2** | Date range – update | `GET /biosamples/samples?filter=dt:update:from=2018-01-01` | 200; samples updated on or after 2018-01-01 | |
| **S3.3** | Relationship filter | `GET /biosamples/samples?filter=rel:derived+from:SAMEA7992418` | 200; samples that have “derived from” relationship to given accession | |
| **S3.4** | Inverse relationship | `GET /biosamples/samples?filter=rrel:has+member:SAMEG316651` | 200; samples that are target of “has member” from given accession | |
| **S3.5** | External reference – archive only | `GET /biosamples/samples?filter=extd:ENA` | 200; samples with at least one ENA external reference | |
| **S3.6** | External reference – archive + id | `GET /biosamples/samples?filter=extd:ENA:SRS359918` | 200; samples linked to ENA accession SRS359918 | |
| **S3.7** | Name filter | `GET /biosamples/samples?filter=name:<known_sample_name>` | 200; samples matching name (case-sensitive per docs) | |

---

### 4.4 Pagination and sort

| ID | Scenario | Steps | Expected result | Pass / Fail |
|----|----------|--------|------------------|-------------|
| **S4.1** | Page-based pagination | `GET /biosamples/samples?page=0&size=20` then `page=1&size=20` | 200; first 20 then next 20; no duplicate accessions across pages | |
| **S4.2** | Cursor-based pagination | `GET /biosamples/samples?cursor=*&size=20`; then use `next` link cursor for next page | 200; stable ordering; no duplicates; cursor format `update,accession` (or as returned) | |
| **S4.3** | Sort by update desc | `GET /biosamples/samples?page=0&size=10&sort=update,desc` | 200; results ordered by update date descending | |
| **S4.4** | Sort by accession asc | `GET /biosamples/samples?page=0&size=10&sort=accession,asc` | 200; results ordered by accession ascending | |
| **S4.5** | Pagination limits | `GET /biosamples/samples?page=501&size=20` | 400 or 4xx; pagination exception (page > 500) | |
| **S4.6** | Size limit (page) | `GET /biosamples/samples?page=0&size=201` | 400 or 4xx; size > 200 rejected for page-based | |

---

### 4.5 Combined text + filters

| ID | Scenario | Steps | Expected result | Pass / Fail |
|----|----------|--------|------------------|-------------|
| **S5.1** | Text + attribute filter | `GET /biosamples/samples?text=human&filter=attr:project:FAANG` | 200; samples matching “human” and in project FAANG | |
| **S5.2** | Text + date filter | `GET /biosamples/samples?text=cancer&filter=dt:update:from=2020-01-01` | 200; samples matching “cancer” and updated since 2020-01-01 | |
| **S5.3** | Multiple filters (AND) | `GET /biosamples/samples?filter=attr:organism:Homo sapiens&filter=attr:project:HipSci` | 200; samples that satisfy both filters | |

---

### 4.6 Content negotiation and representation

| ID | Scenario | Steps | Expected result | Pass / Fail |
|----|----------|--------|------------------|-------------|
| **S6.1** | HAL+JSON | `GET /biosamples/samples?size=5` with `Accept: application/hal+json` | 200; HAL collection with `_embedded`, `_links` | |
| **S6.2** | JSON | `GET /biosamples/samples?size=5` with `Accept: application/json` | 200; JSON collection (HAL or plain list per implementation) | |
| **S6.3** | applyCurations=true | `GET /biosamples/samples?size=5&applyCurations=true` | 200; samples include applied curations | |
| **S6.4** | applyCurations=false | `GET /biosamples/samples?size=5&applyCurations=false` | 200; samples without curations applied | |

---

### 4.7 Authentication and visibility (if WEBIN available)

| ID | Scenario | Steps | Expected result | Pass / Fail |
|----|----------|--------|------------------|-------------|
| **S7.1** | Unauthenticated – no private samples | `GET /biosamples/samples?text=<name_of_private_sample>` (no auth) | 200; private sample not in results | |
| **S7.2** | Authenticated – own private samples | `GET /biosamples/samples` with WEBIN Bearer token | 200; submitter’s private/draft samples appear where applicable | |
| **S7.3** | Domain filter (authenticated) | `GET /biosamples/samples?filter=dom:self.<domain>` with valid auth | 200; only samples in that domain (per docs) | |

---

### 4.8 Structured data filter (if supported in ES)

| ID | Scenario | Steps | Expected result | Pass / Fail |
|----|----------|--------|------------------|-------------|
| **S8.1** | Structured data type filter | `GET /biosamples/samples?filter=strd:<data_type>` (e.g. AMR, CHICKEN_DATA) | 200; samples that have structured data of that type | |

---

### 4.9 Error and edge cases

| ID | Scenario | Steps | Expected result | Pass / Fail |
|----|----------|--------|------------------|-------------|
| **S9.1** | Invalid filter format | `GET /biosamples/samples?filter=invalid:filter` | 400 or 4xx with clear error message | |
| **S9.2** | biosamples-search unavailable | Stop biosamples-search; `GET /biosamples/samples` | 500 or 503; no silent fallback to stale data; error logged | |
| **S9.3** | Empty result set | `GET /biosamples/samples?text=nonexistentterm12345&filter=acc:SAMN99999999` | 200; empty list; total count 0; valid links | |

---

### 4.10 Elasticsearch-specific acceptance

| ID | Scenario | Steps | Expected result | Pass / Fail |
|----|----------|--------|------------------|-------------|
| **S10.1** | Cursor stability | Same query twice with cursor pagination; compare first two pages | Same accessions in same order (no flip-flop between requests) | |
| **S10.2** | Total count consistency | For a given query, compare `totalElements` (or equivalent) across page vs cursor | Same total count for same query/filters | |
| **S10.3** | Sort consistency | Request sort `update,desc`; verify first result has update ≥ second ≥ third | Ordering consistent with requested sort | |

---

## 5. Sign-off

| Role | Name | Date | Signature |
|------|------|------|------------|
| Test executor | | | |
| Product owner / Stakeholder | | | |
| Development lead | | | |

---

## 6. Revision history

| Version | Date | Author | Changes |
|--------|------|--------|---------|
| 1.0 | Feb 2025 | | Initial UAT for Elasticsearch search migration |

---

*This UAT is aligned with the BioSamples Search guide, Filters reference, and the biosamples-search (Elasticsearch) integration in the BioSamples API.*
