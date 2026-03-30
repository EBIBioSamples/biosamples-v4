#!/usr/bin/env bash
# Builds a local checkout of biosamples-search into a Docker image.

set -euo pipefail

# Configuration (override via environment variables if needed)
# By default, assume a sibling checkout of biosamples-search next to this repo.
# If you want to hard-code an absolute path, use:
REPO_DIR="/mnt/c/users/dgupta/biosamples-peripheral-services/biosamples-search"
#REPO_DIR="${REPO_DIR:-../biosamples-peripheral-services/biosamples-search}"
IMAGE_TAG="${IMAGE_TAG:-biosamples-search:latest}"
DOCKER_CONTEXT="${DOCKER_CONTEXT:-}"   # If empty, auto-detect a Dockerfile
GRADLE_TASKS="${GRADLE_TASKS:-build}"  # e.g., assemble, build
GRADLE_ARGS="${GRADLE_ARGS:---no-daemon -x test}"  # add/remove -x test as needed
USE_DOCKER_GRADLE="${USE_DOCKER_GRADLE:-1}"        # 1 = use Dockerized Gradle (JDK 24), 0 = use local Gradle
GRADLE_DOCKER_IMAGE="${GRADLE_DOCKER_IMAGE:-gradle:8.14-jdk24}"  # Gradle with JDK 24

# Checks
command -v docker >/dev/null 2>&1 || { echo "docker is required"; exit 1; }

echo "[1/3] Using local biosamples-search checkout: $REPO_DIR"
if [ ! -d "$REPO_DIR" ]; then
  echo "Local biosamples-search directory '$REPO_DIR' does not exist."
  echo "Set REPO_DIR to the path of your local biosamples-search checkout."
  exit 1
fi

cd "$REPO_DIR"


echo "[2/3] Detecting Dockerfile"
if [ -n "${DOCKER_CONTEXT}" ] && [ -f "${DOCKER_CONTEXT}/Dockerfile" ]; then
  CONTEXT_DIR="${DOCKER_CONTEXT}"
else
  DETECTED=$(find . -maxdepth 3 -type f -name Dockerfile | head -n 1 || true)
  if [ -n "$DETECTED" ]; then
    CONTEXT_DIR="$(dirname "$DETECTED")"
  else
    echo "No Dockerfile found. Set DOCKER_CONTEXT to the directory that contains a Dockerfile."
    exit 1
  fi
fi

echo "[3/3] Building Docker image: ${IMAGE_TAG}"
# Check if buildx is available, otherwise use regular docker build
if docker buildx version >/dev/null 2>&1; then
  echo "Using BuildKit (buildx available)"
  DOCKER_BUILDKIT=1 docker build -t "${IMAGE_TAG}" "${CONTEXT_DIR}"
else
  echo "BuildKit not available, using standard docker build"
  docker build -t "${IMAGE_TAG}" "${CONTEXT_DIR}"
fi

echo "${IMAGE_TAG} built successfully"

docker compose up -d elastic

cd -