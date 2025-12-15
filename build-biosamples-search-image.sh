#!/usr/bin/env bash
# Clones EBIBioSamples/biosamples-search, builds with Gradle (Java 24), and builds a Docker image.

set -euo pipefail

# Configuration (override via environment variables if needed)
REPO_URL="${REPO_URL:-https://github.com/EBIBioSamples/biosamples-search}"
CLONE_DIR="${CLONE_DIR:-./temp/biosamples-search}"
IMAGE_TAG="${IMAGE_TAG:-biosamples-search:latest}"
DOCKER_CONTEXT="${DOCKER_CONTEXT:-}"   # If empty, auto-detect a Dockerfile
GRADLE_TASKS="${GRADLE_TASKS:-build}"  # e.g., assemble, build
GRADLE_ARGS="${GRADLE_ARGS:---no-daemon -x test}"  # add/remove -x test as needed
USE_DOCKER_GRADLE="${USE_DOCKER_GRADLE:-1}"        # 1 = use Dockerized Gradle (JDK 24), 0 = use local Gradle
GRADLE_DOCKER_IMAGE="${GRADLE_DOCKER_IMAGE:-gradle:8.14-jdk24}"  # Gradle with JDK 24

# Checks
command -v git >/dev/null 2>&1 || { echo "git is required"; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "docker is required"; exit 1; }

echo "[1/3] Cloning repository: $REPO_URL"
if [ -d "$CLONE_DIR" ]; then
  echo "Directory '$CLONE_DIR' already exists. Using it."
else
  git clone --depth 1 "$REPO_URL" "$CLONE_DIR"
fi

cd "$CLONE_DIR"


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
rm -rf ./temp/biosamples-search