# BioSamples V2 API Usage

This guide documents the BioSamples V2 endpoints implemented by `webapps/core-v2`.
Paths below are relative to the V2 base URL.

```python
BASE_URL = "https://www.ebi.ac.uk/biosamples/v2"
# Development:
# BASE_URL = "https://wwwdev.ebi.ac.uk/biosamples/v2"
```

For local Docker Compose, the V2 app is exposed at:

```python
BASE_URL = "http://localhost:8082/biosamples/v2"
```

## Authentication

Most V2 endpoints require an ENA Webin JWT token in the `Authorization` header.
Get the token from ENA Webin Auth, then send it as a bearer token.

```python
import os
import requests

WEBIN_AUTH_URL = "https://www.ebi.ac.uk/ena/submit/webin/auth/token"

token_response = requests.post(
    WEBIN_AUTH_URL,
    json={
        "authRealms": ["ENA"],
        "username": os.environ["WEBIN_USERNAME"],
        "password": os.environ["WEBIN_PASSWORD"],
    },
    timeout=30,
)
token_response.raise_for_status()
TOKEN = token_response.text.strip()

HEADERS = {
    "Accept": "application/json",
    "Content-Type": "application/json",
    "Authorization": f"Bearer {TOKEN}",
}
```

Use `https://wwwdev.ebi.ac.uk/ena/submit/webin/auth/token` for development Webin accounts.

## Sample Payload

Submission endpoints accept a JSON array of sample objects. New samples should include at least `name`, `release`, and relevant `characteristics`.

```python
sample = {
    "name": "example-sample-001",
    "release": "2030-01-01T00:00:00Z",
    "characteristics": {
        "organism": [
            {
                "text": "Homo sapiens",
                "ontologyTerms": [
                    "http://purl.obolibrary.org/obo/NCBITaxon_9606"
                ],
            }
        ],
        "sample title": [{"text": "Example sample 001"}],
    },
}
```

Useful fields:

| Field | Notes |
| --- | --- |
| `name` | Required by the model. |
| `accession` | Include only when updating an existing sample. Do not include for new normal submissions. |
| `sraAccession` | SRA/ERS accession field. Normal users should not set this. |
| `release` | ISO date or date-time. Future dates make samples private; past/current dates can make them public. |
| `status` | `PRIVATE`, `PUBLIC`, or `SUPPRESSED`, subject to server-side transition rules. |
| `characteristics` | Object keyed by characteristic name, each value an array of `{text, ontologyTerms, unit, tag}` objects. |
| `relationships` | Restricted for normal Webin users in V2 submission paths. |
| `submittedVia` | Optional; defaults to `JSON_API`. |

## Endpoint Summary

| Method | Path | Auth | Success | Purpose |
| --- | --- | --- | --- | --- |
| `GET` | `/samples/{accession}` | Required | `200 OK` | Fetch one sample by accession. |
| `GET` | `/samples/bulk-fetch` | Required | `200 OK` | Fetch many samples by accession. |
| `POST` | `/samples/bulk-accession` | Required | `200 OK` | Pre-accession new samples and save them as private. |
| `POST` | `/samples/bulk-submit-get-receipt` | Required | `201 Created` | Validate and submit samples, returning successes and per-sample errors. |
| `POST` | `/samples/bulk-submit` | Superuser only | `201 Created` | Submit without schema validation. |
| `POST` | `/samples/bulk-validate` | Optional at security layer; token recommended | `200 OK` | Validate samples without persisting them. |
| `POST` | `/samples/generateSRAAccession` | Required by security config for `/samples/**` POST routes | `200 OK` | Generate one SRA accession string. |

## GET One Sample

```python
import requests

accession = "SAMEA123456789"
response = requests.get(
    f"{BASE_URL}/samples/{accession}",
    headers=HEADERS,
    timeout=30,
)
response.raise_for_status()

sample = response.json()
print(sample["name"], sample.get("accession"))
```

Private samples are returned only when the authenticated Webin account is allowed to see them.

## Bulk Fetch Samples

Pass repeated `accessions` query parameters. `requests` does this automatically when the parameter value is a list.

```python
accessions = ["SAMEA123456789", "SAMEA987654321"]

response = requests.get(
    f"{BASE_URL}/samples/bulk-fetch",
    headers=HEADERS,
    params={"accessions": accessions},
    timeout=60,
)
response.raise_for_status()

samples_by_accession = response.json()
for accession, sample in samples_by_accession.items():
    print(accession, sample["name"])
```

Response shape:

```json
{
  "SAMEA123456789": {
    "name": "example-sample-001",
    "accession": "SAMEA123456789",
    "release": "2030-01-01T00:00:00Z",
    "characteristics": {
      "organism": [
        {
          "text": "Homo sapiens",
          "ontologyTerms": ["http://purl.obolibrary.org/obo/NCBITaxon_9606"]
        }
      ]
    }
  }
}
```

Missing or inaccessible samples are omitted from the response. Compare the requested accessions with the response keys to detect gaps.

## Bulk Accession

Use bulk accession when a client needs BioSamples accessions before the full metadata submission.

Do not include:

- `accession`
- `sraAccession`
- a characteristic named `SRA accession`

```python
samples = [
    {
        "name": "example-preaccession-001",
        "release": "2030-01-01T00:00:00Z",
        "characteristics": {
            "organism": [
                {
                    "text": "Homo sapiens",
                    "ontologyTerms": [
                        "http://purl.obolibrary.org/obo/NCBITaxon_9606"
                    ],
                }
            ]
        },
    }
]

response = requests.post(
    f"{BASE_URL}/samples/bulk-accession",
    headers=HEADERS,
    json=samples,
    timeout=60,
)
response.raise_for_status()

accessions_by_name = response.json()
print(accessions_by_name)
```

Example response:

```json
{
  "example-preaccession-001": "SAMEA123456789"
}
```

The endpoint saves samples as private and returns a JSON object keyed by submitted sample name.

## Bulk Submit With Receipt

Use this for normal bulk submission. The endpoint validates each sample, persists successful samples, and returns a receipt containing both successes and failures.

```python
samples = [sample]

response = requests.post(
    f"{BASE_URL}/samples/bulk-submit-get-receipt",
    headers=HEADERS,
    json=samples,
    timeout=120,
)
response.raise_for_status()

receipt = response.json()

for created in receipt.get("samples") or []:
    print("submitted", created["name"], created.get("accession"))

for item in receipt.get("errors") or []:
    print("failed", item["sampleName"])
    for error in item.get("errors", []):
        print(error.get("dataPath", ""), error.get("errors", []))
```

Example response:

```json
{
  "samples": [
    {
      "name": "example-sample-001",
      "accession": "SAMEA123456789",
      "release": "2030-01-01T00:00:00Z"
    }
  ],
  "errors": [
    {
      "sampleName": "example-sample-002",
      "errors": [
        {
          "dataPath": "$.characteristics.organism",
          "errors": ["organism is required"]
        }
      ]
    }
  ]
}
```

`201 Created` means the request completed. It does not mean every sample succeeded. Always inspect `errors`.

## Bulk Validate

Use validation before submitting a large batch. This endpoint returns a receipt-like body and does not persist samples.

```python
response = requests.post(
    f"{BASE_URL}/samples/bulk-validate",
    headers=HEADERS,
    json=[sample],
    timeout=120,
)
response.raise_for_status()

receipt = response.json()
if receipt.get("errors"):
    for item in receipt["errors"]:
        print("failed", item["sampleName"], item["errors"])
else:
    print("all samples passed validation")
```

Successful validation response:

```json
{
  "samples": null,
  "errors": []
}
```

## Bulk Submit Without Validation

`POST /samples/bulk-submit` persists samples without schema validation. It is restricted to the configured Webin superuser.

```python
response = requests.post(
    f"{BASE_URL}/samples/bulk-submit",
    headers=HEADERS,
    json=[sample],
    timeout=120,
)
response.raise_for_status()

created_samples = response.json()
for created in created_samples:
    print(created["name"], created.get("accession"))
```

Regular Webin users should use `/samples/bulk-submit-get-receipt` instead. Non-superuser calls return `406 Not Acceptable`.

## Generate One SRA Accession

```python
response = requests.post(
    f"{BASE_URL}/samples/generateSRAAccession",
    headers=HEADERS,
    timeout=30,
)
response.raise_for_status()

sra_accession = response.text
print(sra_accession)
```

This returns a plain text accession string.

## Small Reusable Client

```python
from typing import Any, Iterable

import requests


class BioSamplesV2Client:
    def __init__(self, base_url: str, token: str, timeout: int = 60) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        self.session.headers.update(
            {
                "Accept": "application/json",
                "Content-Type": "application/json",
                "Authorization": f"Bearer {token}",
            }
        )

    def get_sample(self, accession: str) -> dict[str, Any]:
        response = self.session.get(
            f"{self.base_url}/samples/{accession}",
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()

    def bulk_fetch(self, accessions: Iterable[str]) -> dict[str, dict[str, Any]]:
        response = self.session.get(
            f"{self.base_url}/samples/bulk-fetch",
            params={"accessions": list(accessions)},
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()

    def bulk_accession(self, samples: list[dict[str, Any]]) -> dict[str, str]:
        response = self.session.post(
            f"{self.base_url}/samples/bulk-accession",
            json=samples,
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()

    def bulk_submit_get_receipt(self, samples: list[dict[str, Any]]) -> dict[str, Any]:
        response = self.session.post(
            f"{self.base_url}/samples/bulk-submit-get-receipt",
            json=samples,
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()

    def bulk_validate(self, samples: list[dict[str, Any]]) -> dict[str, Any]:
        response = self.session.post(
            f"{self.base_url}/samples/bulk-validate",
            json=samples,
            timeout=self.timeout,
        )
        response.raise_for_status()
        return response.json()
```

Usage:

```python
client = BioSamplesV2Client(BASE_URL, TOKEN)
receipt = client.bulk_validate([sample])
print(receipt)
```

## Operational Notes

- V2 controllers live under `webapps/core-v2`.
- The application context path is expected to be `/biosamples/v2`; controller paths start at `/samples`.
- Normal users cannot submit samples containing existing BioSamples accessions, SRA accessions, or relationships through the standard V2 bulk submission flow.
- Superuser behavior differs: superusers can update existing samples and can use the no-validation submit endpoint.
- Release dates influence status. Future-release samples are private; reached release dates can make samples public.
- Fetch endpoints return samples without curations applied.
- For batch workflows, validate first, submit second, then inspect the receipt for partial failures.
