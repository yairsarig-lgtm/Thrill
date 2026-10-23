#!/usr/bin/env bash
# Builds FreeWill.apk without Gradle: aapt2 -> javac -> d8 -> pack + v2 sign.
set -euo pipefail
cd "$(dirname "$0")"
TOOLS=${TOOLS:-/tmp/claude-0/dl/tools}
ANDROID_JAR=$TOOLS/android.jar
D8_CP="$TOOLS/d8.jar:$(ls $TOOLS/lib/*.jar | tr '\n' ':')"
OUT=out
rm -rf $OUT && mkdir -p $OUT/res $OUT/gen $OUT/classes $OUT/dex

$TOOLS/aapt2 compile --dir res -o $OUT/res/
$TOOLS/aapt2 link -o $OUT/base.apk -I $ANDROID_JAR --manifest AndroidManifest.xml -A assets \
    --java $OUT/gen $OUT/res/*.flat

javac -nowarn --release 8 -cp $ANDROID_JAR -d $OUT/classes \
    $(find src $OUT/gen -name '*.java') 2>&1 | grep -v -E 'bootstrap|source value 8|target value 8|To suppress|^[0-9]+ warning' || true
test -f $OUT/classes/com/freewill/predict/PredictionKeyboard.class

mkdir -p $OUT/tool && javac -nowarn -cp "$TOOLS/lib/asm-7.1.jar" -d $OUT/tool tools/StripParams.java
java -cp "$OUT/tool:$TOOLS/lib/asm-7.1.jar" StripParams $(find $OUT/classes -name '*.class')

java -cp "$D8_CP" com.android.tools.r8.D8 --release --min-api 26 --lib $ANDROID_JAR \
    --output $OUT/dex $(find $OUT/classes -name '*.class')

python3 tools/pack_sign.py $OUT/base.apk $OUT/dex/classes.dex signing-key.pem FreeWill.apk
