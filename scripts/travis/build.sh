#!/bin/bash

sbt_cmd="sbt ++$TRAVIS_SCALA_VERSION"

build_all="$sbt_cmd clean test scalafmtCheck scalafmtSbtCheck"

eval $build_all