#!/bin/bash

sbt_cmd="sbt ++$TRAVIS_SCALA_VERSION"

build_all="$sbt_cmd verify"

eval $build_all