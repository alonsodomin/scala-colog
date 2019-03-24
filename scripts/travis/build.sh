#!/bin/bash

set -e

sbt_cmd="sbt ++$TRAVIS_SCALA_VERSION"

build_and_test="$sbt_cmd validateModules validateDocs"
eval "${build_and_test}"

if [[ ! -z "$CODACY_PROJECT_TOKEN" ]]; then
    eval "${sbt_cmd} codacyCoverage"
fi

