#!/usr/bin/env sh
set -eu

RELEASE_VERSION="${1:-$(git rev-parse --short HEAD 2>/dev/null || date +%Y%m%d%H%M%S)}"

./gradlew clean :service-a:bootJar :service-b:bootJar

docker build -t "rpc-multi-service/service-a:${RELEASE_VERSION}" ./service-a
docker build -t "rpc-multi-service/service-b:${RELEASE_VERSION}" ./service-b

printf 'RELEASE_VERSION=%s\n' "${RELEASE_VERSION}" > .release.env

echo "Built immutable release images:"
echo "  rpc-multi-service/service-a:${RELEASE_VERSION}"
echo "  rpc-multi-service/service-b:${RELEASE_VERSION}"
echo "Release env written to .release.env"
