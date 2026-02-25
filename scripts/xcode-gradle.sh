#!/bin/sh
# xcode-gradle.sh — Wrapper that resolves JAVA_HOME before invoking gradlew.
#
# Problem: Xcode IDE runs build-phase scripts in a sanitized environment
# that lacks shell PATH customisations (Nix, SDKMAN, Homebrew, etc.).
# The Gradle wrapper needs a JVM to bootstrap, but org.gradle.java.home
# in local.properties is only read *after* the JVM is already running.
#
# Solution: Parse local.properties for org.gradle.java.home and export
# it as JAVA_HOME so gradlew can find the JVM at bootstrap time.

if [ -z "$JAVA_HOME" ]; then
  LP="$SRCROOT/../local.properties"
  if [ -f "$LP" ]; then
    JAVA_HOME=$(grep '^org\.gradle\.java\.home=' "$LP" | cut -d'=' -f2-)
    if [ -n "$JAVA_HOME" ]; then
      export JAVA_HOME
    fi
  fi
fi

exec "$SRCROOT/../gradlew" "$@"
