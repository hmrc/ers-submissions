#!/usr/bin/env bash

sbt clean scalastyleAll compile coverage testAll coverageOff coverageReport dependencyUpdates