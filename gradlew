#!/usr/bin/env bash

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Stop on error
set -e

# The real distribution is determined by the wrapper configuration file
# This is a fallback for the case that the download of the wrapper fails
# and the wrapper configuration is not available
DEFAULT_GRADLE_DISTRIBUTION="gradle-8.0-bin"
DEFAULT_GRADLE_DISTRIBUTION_URL="https://services.gradle.org/distributions/${DEFAULT_GRADLE_DISTRIBUTION}.zip"

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=()

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    DEFAULT_JVM_OPTS+=("-Xdock:name=$APP_NAME" "-Xdock:icon=$APP_HOME/media/gradle.icns")
fi

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
        exit 1
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || (echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2 && echo "Please set the JAVA_HOME variable in your environment to match the location of your Java installation." >&2 && exit 1)
fi

# Check for the wrapper configuration
WRAPPER_PROPS="${APP_HOME}/gradle/wrapper/gradle-wrapper.properties"
if [ -f "$WRAPPER_PROPS" ]; then
    # shellcheck disable=SC2002
    GRADLE_DISTRIBUTION_URL=`cat "$WRAPPER_PROPS" | grep "distributionUrl" | cut -d'=' -f2`
else
    # shellcheck disable=SC2154
    echo "WARNING: gradle-wrapper.properties not found, using default distribution URL: $DEFAULT_GRADLE_DISTRIBUTION_URL" >&2
    GRADLE_DISTRIBUTION_URL="$DEFAULT_GRADLE_DISTRIBUTION_URL"
fi
# shellcheck disable=SC2154
if [ -z "$GRADLE_DISTRIBUTION_URL" ]; then
    echo "WARNING: distributionUrl not found in gradle-wrapper.properties, using default distribution URL: $DEFAULT_GRADLE_DISTRIBUTION_URL" >&2
    GRADLE_DISTRIBUTION_URL="$DEFAULT_GRADLE_DISTRIBUTION_URL"
fi
# Remove the backslash from the colon in the URL
GRADLE_DISTRIBUTION_URL=`echo ${GRADLE_DISTRIBUTION_URL} | sed 's/\\\:/:/'`

# Check for the wrapper jar
WRAPPER_JAR="${APP_HOME}/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
    # Create the directories if they don't exist
    mkdir -p "${APP_HOME}/gradle/wrapper"
    # Download the wrapper jar
    echo "Downloading $GRADLE_DISTRIBUTION_URL" >&2
    if [ `command -v curl` ]; then
        # use curl
        DOWNLOAD_ERR=`curl -f -L -sS -o "$WRAPPER_JAR" "$GRADLE_DISTRIBUTION_URL" 2>&1`
    elif [ `command -v wget` ]; then
        # use wget
        DOWNLOAD_ERR=`wget -q -O "$WRAPPER_JAR" "$GRADLE_DISTRIBUTION_URL" 2>&1`
    else
        # try java
        echo "No external download tool found. Trying to use java." >&2
        # an awkward way to download with Java
        # echo "System.out.println(new sun.misc.BASE64Encoder().encode(new java.io.ByteArrayOutputStream(){{ read(new java.net.URL(new String(new sun.misc.BASE64Decoder().decodeBuffer(\"`echo -n "$GRADLE_DISTRIBUTION_URL" | base64`\"))).openStream(), this); }}.toByteArray()));" | "$JAVACMD" -cp . - > "$WRAPPER_JAR".base64
        # "$JAVACMD" -cp . sun.misc.BASE64Decoder < "$WRAPPER_JAR".base64 > "$WRAPPER_JAR"
        # rm "$WRAPPER_JAR".base64
        DOWNLOAD_ERR="Java download not supported. Please install curl or wget"
    fi
    if [ $? -ne 0 ] || [ ! -f "$WRAPPER_JAR" ]; then
        # Download failed
        echo "Failed to download Gradle wrapper jar. Please check your network connection." >&2
        echo "Error message: $DOWNLOAD_ERR" >&2
        exit 1
    fi
fi
# Collect all arguments for the java command, following the shell quoting and substitution rules
eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "\"-Dorg.gradle.appname=$APP_BASE_NAME\"" -classpath "\"$WRAPPER_JAR\"" org.gradle.wrapper.GradleWrapperMain "$@"

# by default we should be in the correct project dir, but when run from Finder on Mac, the cwd is wrong
if [ "$(uname)" = "Darwin" ] && [ "$HOME" = "$PWD" ]; then
  cd "$(dirname "$0")"
fi

exec "$JAVACMD" "$@"
