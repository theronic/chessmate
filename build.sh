#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build
javac -d build src/*.java
if [ "${1:-}" = "test" ]; then
    javac -cp build -d build tests/*.java
    java -Xmx512m -Djava.awt.headless=true -cp build Chess.ChessTest
elif [ "${1:-}" = "run" ]; then
    cd src
    exec java -cp ../build Chess.Main
fi
