#!/bin/bash

sbt_cmd="sbt ++$TRAVIS_SCALA_VERSION"

build_all="$sbt_cmd clean test"

eval $build_all