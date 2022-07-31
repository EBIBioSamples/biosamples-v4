# 5. 'release' and 'update' date - mandatory  or not

Date: 2019-01-17

## Status
Accepted

## Context
Sample should have a release and update date when we store it in database.
When user submit a sample we could give flexibility to the user by filling release and update date to today if they are missing.
But filling release date can accidentally make a sample public.  

## Decision
We decided that it is best to send an error message if sample release date is not provided.
On the other hand, we will fill update date to today's date if it is missing.

## Consequences
* Prevents unintentional submissions with missing release date.
* User must provide at least sample name and release date when submitting a sample.
