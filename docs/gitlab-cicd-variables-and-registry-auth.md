# GitLab CI/CD Variables and Registry Auth

This note summarizes the GitLab CI/CD variables that matter for this repository's build and deploy flow, with emphasis on container registry authentication and how `docker-registry-secret` is generated.

## 1. GitLab CI/CD variable types

GitLab CI/CD variables usually fall into four buckets:

1. Predefined variables: created by GitLab for every pipeline or job.
2. Project or group variables: created in GitLab UI or API and injected into jobs.
3. Job-level variables: declared in `.gitlab-ci.yml` globally or per job.
4. File variables: exposed as temporary files rather than plain environment values.

GitLab also distinguishes variables by when they are available:

1. Pre-pipeline: available before the pipeline is created.
2. Pipeline: available while GitLab is creating the pipeline.
3. Job-only: available only when a runner executes a job.

For container registry auth, the important detail is that `CI_REGISTRY_USER` and `CI_REGISTRY_PASSWORD` are job-time credentials, not stable cluster credentials.

## 2. Variables used by this repository

The pipeline in [`.gitlab-ci.yml`](</C:/Users/dgupta/biosamples-2/biosamples-v4/.gitlab-ci.yml:5>) mixes GitLab predefined variables, repository-defined variables, and manually managed secrets.

### Repository-defined variables in `.gitlab-ci.yml`

These are declared in the top-level `variables:` block:

| Variable | Purpose |
| --- | --- |
| `DEPLOY_PATH` | General deploy path, not central to the Kubernetes flow shown here. |
| `DOCKER_DRIVER` | Docker storage driver for CI jobs using Docker-in-Docker. |
| `DOCKER_TLS_CERTDIR` | Disables DinD TLS auto-setup in this pipeline. |
| `DOCKER_TAG` | Image tag built from `$CI_COMMIT_REF_SLUG-$CI_COMMIT_SHORT_SHA`. |
| `DOCKER_IMAGE_NAME` | Default image name pattern based on `$CI_REGISTRY_IMAGE`. |
| `DOCKER_PULL_SECRET` | Name of the Kubernetes image pull secret. Set to `docker-registry-secret`. |
| `APP_NAME` | Defaults to `$CI_PROJECT_NAME`. |
| `K8S_NAMESPACE_PREFIX` | Namespace prefix for BioSamples environments. |

### Job-level variables used for deploy targets

These are set per job:

| Variable | Purpose |
| --- | --- |
| `ENVIRONMENT_NAME` | Selects the config set or values file for the target environment. |
| `K8S_NAMESPACE` | Tells `kubectl` and Helm which namespace to target. |

Examples appear in [`.gitlab-ci.yml`](</C:/Users/dgupta/biosamples-2/biosamples-v4/.gitlab-ci.yml:89>) through [`.gitlab-ci.yml`](</C:/Users/dgupta/biosamples-2/biosamples-v4/.gitlab-ci.yml:197>).

### Predefined GitLab variables used here

| Variable | Purpose in this repo |
| --- | --- |
| `CI_REGISTRY` | Registry hostname used by `docker login` and Kubernetes secret creation. |
| `CI_REGISTRY_IMAGE` | Base image path used when building and deploying application images. |
| `CI_REGISTRY_USER` | Registry username supplied by GitLab at job runtime. |
| `CI_REGISTRY_PASSWORD` | Registry password or token supplied by GitLab at job runtime. |
| `CI_COMMIT_REF_SLUG` | Used in `DOCKER_TAG` and branch-latest image tags. |
| `CI_COMMIT_SHORT_SHA` | Used in `DOCKER_TAG`. |
| `CI_PROJECT_NAME` | Used to default `APP_NAME`. |
| `CI_PROJECT_DIR` | Echoed in `before_script`. |

### Manually managed variables expected by this repo

| Variable | Purpose |
| --- | --- |
| `BSD_INTERNAL_USER` | Used to clone `biosamples-internal` during the `clone-config` stage. |
| `BSD_INTERNAL_PASS` | Password or token paired with `BSD_INTERNAL_USER`. |

Those appear in [`.gitlab-ci.yml`](</C:/Users/dgupta/biosamples-2/biosamples-v4/.gitlab-ci.yml:76>).

## 3. How registry auth works in this pipeline

The package stage logs in to the GitLab container registry before building and pushing images:

- [`.gitlab-ci.yml`](</C:/Users/dgupta/biosamples-2/biosamples-v4/.gitlab-ci.yml:45>) runs:

```sh
echo "$CI_REGISTRY_PASSWORD" | docker login --username "$CI_REGISTRY_USER" --password-stdin "$CI_REGISTRY"
```

That login is used for `docker push` in the same CI job. It is appropriate for CI-time image publishing.

## 4. How `docker-registry-secret` is generated

For Kubernetes deploy jobs, the pipeline deletes and recreates the pull secret in the target namespace:

- Webapp deploy path: [`.gitlab-ci.yml`](</C:/Users/dgupta/biosamples-2/biosamples-v4/.gitlab-ci.yml:212>) to [`.gitlab-ci.yml`](</C:/Users/dgupta/biosamples-2/biosamples-v4/.gitlab-ci.yml:217>)
- Reindex job deploy path: [`.gitlab-ci.yml`](</C:/Users/dgupta/biosamples-2/biosamples-v4/.gitlab-ci.yml:248>) to [`.gitlab-ci.yml`](</C:/Users/dgupta/biosamples-2/biosamples-v4/.gitlab-ci.yml:253>)

The command is:

```sh
kubectl create secret docker-registry $DOCKER_PULL_SECRET \
  --docker-server=$CI_REGISTRY \
  --docker-username=$CI_REGISTRY_USER \
  --docker-password=$CI_REGISTRY_PASSWORD
```

With the current top-level defaults, this resolves to a secret named `docker-registry-secret`.

### What value ends up in the secret

Kubernetes stores this as a secret of type `kubernetes.io/dockerconfigjson`. The important data field is `.dockerconfigjson`, which is a base64-encoded Docker config JSON object shaped like this:

```json
{
  "auths": {
    "<registry-host>": {
      "username": "<docker-username>",
      "password": "<docker-password-or-token>",
      "auth": "<base64(username:password)>"
    }
  }
}
```

So the value of `docker-registry-secret` is generated directly from these runtime values:

1. `CI_REGISTRY`
2. `CI_REGISTRY_USER`
3. `CI_REGISTRY_PASSWORD`

There is no separate `DOCKER_AUTH_CONFIG` variable in this repository. The secret payload is just the Kubernetes-generated Docker config JSON equivalent of those three inputs.

## 5. How pods use that secret

The Helm job chart injects `imagePullSecrets` into the pod spec in [k8s/jobs/templates/job.yaml](</C:/Users/dgupta/biosamples-2/biosamples-v4/k8s/jobs/templates/job.yaml:18>).

The deploy job passes the secret name into Helm here:

- [`.gitlab-ci.yml`](</C:/Users/dgupta/biosamples-2/biosamples-v4/.gitlab-ci.yml:257>) for the reindex job
- [`.gitlab-ci.yml`](</C:/Users/dgupta/biosamples-2/biosamples-v4/.gitlab-ci.yml:221>) and nearby lines for the webapps

The flow is:

1. CI job receives registry credentials from GitLab.
2. CI job creates `docker-registry-secret` in the target namespace.
3. Helm sets `imagePullSecrets[0].name=docker-registry-secret`.
4. Kubernetes kubelet uses that secret when pulling the image from `dockerhub.ebi.ac.uk`.

## 6. Operational risk in the current design

The current design assumes that a job-time GitLab registry credential can be reused later by Kubernetes for image pulls.

That is fragile if `CI_REGISTRY_USER` and `CI_REGISTRY_PASSWORD` resolve to short-lived job credentials. In that case:

1. The CI job can successfully create the secret.
2. The secret can exist in Kubernetes.
3. A later pod pull can still fail with `unauthorized` because the embedded credential is no longer valid.

This is consistent with secrets that decode to usernames such as `gitlab-ci-token`, which indicate job-token-style auth rather than a stable deploy credential.

## 7. Recommended stable pattern

For Kubernetes image pull secrets, prefer a long-lived read-only registry credential:

1. GitLab deploy token with `read_registry`
2. Project access token with `read_registry`
3. Group access token with `read_registry`

In GitLab CI, the cleanest built-in variables for a deploy token are:

1. `CI_DEPLOY_USER`
2. `CI_DEPLOY_PASSWORD`

If the cluster needs to keep pulling images after the CI job ends, those are a better fit than `CI_REGISTRY_USER` and `CI_REGISTRY_PASSWORD`.

## 8. External references

- GitLab predefined variables: https://docs.gitlab.com/ci/variables/predefined_variables/
- GitLab container registry authentication: https://docs.gitlab.com/user/packages/container_registry/authenticate_with_container_registry/
