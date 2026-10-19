#!/bin/sh
APP_BASE_NAME=$(basename "$0")

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

PRG="$0"
cd "$(dirname "$PRG")/" >/dev/null
APP_HOME="$(pwd -P)"

exec "$JAVACMD" \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
