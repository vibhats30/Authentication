#!/bin/bash
# Script to use Java 21 for this project
# Run: source ./use-java-21.sh

export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

echo "Java version switched to:"
java -version
echo ""
echo "You can now run Maven commands like:"
echo "  mvn clean install"
echo "  mvn spring-boot:run"
echo "  mvn test"
