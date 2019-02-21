#!/bin/bash

sbt_cmd="sbt ++$TRAVIS_SCALA_VERSION"

build_and_test="$sbt_cmd clean coverage test"

eval $build_and_test