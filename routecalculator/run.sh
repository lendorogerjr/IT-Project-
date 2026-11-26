#!/usr/bin/env bash
# Bouwt het project (alleen als er iets veranderd is) en voert het uit.
#   bash run.sh          de interface starten (kaarteditor + route)
#   bash run.sh test     alle testgevallen draaien
set -e
cd "$(dirname "$0")"

if ! command -v kotlinc >/dev/null 2>&1; then
  echo "kotlinc is niet gevonden. Installeer het met:  brew install openjdk kotlin" >&2
  exit 1
fi

JAR=build/routecalculator.jar
mkdir -p build
if [ ! -f "$JAR" ] || [ -n "$(find src -name '*.kt' -newer "$JAR")" ]; then
  echo "Bouwen (de eerste keer duurt dit even)..."
  kotlinc src/main/kotlin src/test/kotlin -include-runtime -d "$JAR" 2>&1 \
    | grep -v "JAVA_TOOL_OPTIONS\|_JAVA_OPTIONS" || true
fi

JAVA="java -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -cp $JAR"
case "${1:-interface}" in
  interface) $JAVA routeberekening.editor.MainKt ;;
  test) $JAVA routeberekening.TestsKt ;;
  *) echo "Gebruik: bash run.sh | bash run.sh test" >&2; exit 1 ;;
esac
