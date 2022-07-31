# 4. Inverse relationships sample update

Date: 2018-10-31

## Status

Accepted

## Context

When a relation is created between two samples, the sample target of the relationship doesn't change the update date.
Stated in another way, the relationship inversion process doesn't change the update date of the sample.
This is associated with BSD-1088 - https://www.ebi.ac.uk/panda/jira/browse/BSD-1088

## Decision

We decided that is good for the relationship inversion process to not change the update date of the sample as we don't 
have any actual usecase for this to happen and it would also create issues for NCBI as they don't care about relationships
and no real information is added to the sample

## Consequences

Keep this in mind if any pipelines will use the inverse relationships to extract data.
