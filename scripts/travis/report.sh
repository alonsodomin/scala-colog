#!/bin/bash

sbt_cmd="sbt ++$TRAVIS_SCALA_VERSION"

report="$sbt_cmd coverageReport"
aggregate="$sbt_cmd coverageAggregate"
codacy="$sbt_cmd codacyCoverage"

if [[ ! -z "$CODACY_PROJECT_TOKEN" ]]; then
    coverage="$report && $aggregate && $codacy"
else
    coverage="$report && $aggregate"
fi

eval $coverage

