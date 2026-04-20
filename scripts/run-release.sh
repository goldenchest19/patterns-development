к#!/usr/bin/env sh
set -eu

if [ -f .release.env ]; then
    docker compose --env-file release.env.example --env-file .release.env -f docker-compose.prod.yml up -d
else
    docker compose --env-file release.env.example -f docker-compose.prod.yml up -d
fi
