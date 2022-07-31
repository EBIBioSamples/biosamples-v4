# 5. Submitting the same sample twice

Date: 2019-01-17

## Status
Accepted

## Context
What should we do if a user submitted same sample twice ? 
1. Update the first one by checking name and domain fields
2. Create another sample with an accession without considering the first one
3. Send an error message explaining that a sample exist with same name and domain

## Decision
For now we decided to create a new sample with a new accession as this updating a sample could cause accidental data loss.

## Consequences
* Duplicate samples will be created in the database
* Prevents accidental updates (which can cause data losses) to a sample 
