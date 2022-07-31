# 3. New Release Process

Date: 2018-10-24

## Status

Accepted

## Context

Updating the release process and branches to give us flexibility in term of fixing issues when a release
is not working fine. What was happening with the current release process is that we had to skip one or more versions 
with issues inside, creating confusion and putting in Maven central invalid versions

## Decision

After long discussion we have decided to update our branches and how we do the release with a new process.

First of all, in order to standardize with other projects (USI in primis) we have decided to use a *dev* branch for 
continuous development and the *master* branch to release new version of the software. Here the process described
step-by-step

*From `dev` to `master`*

- From the `dev` branch we create feature branch - usually associated to a ticket - to work on a specific task
- When the developer has finished the task, he creates a pull request (PR) from the feature branch back into the `dev` branch on github
- If everybody is ok with the PR, it is merged into the `dev` branch
- When the team wants to do a new release of the software, any user can create the release notes and prepare a PR from the `dev` branch to the `master` branch
- When everybody is ok with the release notes, the developer can merge the PR
- Update the version on dev to the next SNAPSHOT (e.g. 4.1.6-SNAPSHOT --> 4.1.7-SNAPSHOT)

*From `master` to release of the software*

- In the `master` branch, the developer needs to update the version of the software to a release candidate (e.g. 4.1.6-SNAPSHOT --> 4.1.6-RC1)
- The software with the corresponding new version is then pushed to the remote master branch, which will trigger a build and release plan on snowy
- If everything is going to be fine, the developer can promote the artifact to scooby and then wwwdev
- After the release on wwwdev, the developer should send an email to the biosamples-users mailing list to announce the new software release and ask people to test it
- If any error is found, the developer need to make an hot-fix on master, update the version of the software (e.g. 4.1.6-RC1 --> 4.1.6-RC2) and release on the various environments till wwwdev
- After the software has been on wwwdev for a week and everybody is happy with it, is time to make the final release
- Update the version to the final version (e.g. 4.1.6-RC2 --> 4.1.6)
- Tag the current commit on github with `git tag v4.1.6` and push both the code and the tag to the remote repository
- Release on all the environments one after the other: snowy --> scooby --> wwwdev --> www
- After the release on production is done, send an email to the biosamples-announcements mailin list announcing the new release
- Merge the `master` branch back into `dev` to make sure `dev` also contains all the hot-fixes done on master

## Consequences

This should make it possible to avoid issues with version of the software not actually released in production and should give 
the flexibility to the team to continue development on a `dev` branch while the release goes on. Also this should make it possible
to release in a faster way as on the `dev` branch developers can continue to work without any issue

