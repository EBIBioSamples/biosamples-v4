# 2. have taxId attribute at top level

Date: 2018-10-23

## Status

Accepted

## Context

ENA presentation requires a top-level, numerical only taxID.

## Decision

We have added a taxID field at the top-level of all biosamples.
- This is a single, top-level, numeric field named 'taxId'.
- If no taxon available the value of taxId will be 0.
- There is no support for multiple taxId as there is no data that meets this requirement.

## Consequences

BioSamples sample JSON always includes a taxId field. 


