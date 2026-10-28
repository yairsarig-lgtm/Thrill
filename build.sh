#!/usr/bin/env bash
# Builds Thrill without Gradle.
#
#   ./build.sh [app-dir] [name]        (defaults: this folder, "Thrill")
#
# Writes two files into app-dir:
#   <name>.apk  sideload build for testers, signed with signing-key.pem
#   <name>.aab  Google Play bundle, signed with the upload key in keystore.properties
#               (skipped, with a note, when keystore.properties is missing)
#
# Pipeline: aapt2 -> javac -> StripParams -> d8 -> (APK: pack + v2 sign)
#                                               -> (AAB: proto resources + bundletool + jarsigner)
set -euo pipefail
ROOT=$(cd "$(dirname "$0")" && pwd)
APP=$(cd "${1:-$ROOT}" && pwd)
NAME=${2:-Thrill}
TOOLS=${TOOLS:-/tmp/claude-0/dl/tools}
ANDROID_JAR=${ANDROID_JAR:-$TOOLS/sdk/android-36.jar}   # compile + link against API 36
BUNDLETOOL=${BUNDLETOOL:-$TOOLS/bt/bundletool.jar}
D8_CP="$TOOLS/d8.jar:$(ls $TOOLS/lib/*.jar | tr '\n' ':')"
R_PKG=com.freewill.predict                               # Java package of the code and of R
quiet() { grep -v -E 'JAVA_TOOL_OPTIONS|bootstrap|source value 8|target value 8|To suppress|^[0-9]+ warning|^Note:' || true; }

cd "$APP"
OUT=out
rm -rf $OUT && mkdir -p $OUT/res $OUT/gen $OUT/classes $OUT/dex $OUT/tool $OUT/module

# ---- resources
$TOOLS/aapt2 compile --dir res -o $OUT/res/
LINK=(-I "$ANDROID_JAR" --manifest AndroidManifest.xml -A assets --custom-package $R_PKG)
$TOOLS/aapt2 link -o $OUT/base.apk "${LINK[@]}" --java $OUT/gen $OUT/res/*.flat
$TOOLS/aapt2 link -o $OUT/proto.apk "${LINK[@]}" --proto-format $OUT/res/*.flat

# ---- code
javac -nowarn --release 8 -cp "$ANDROID_JAR" -d $OUT/classes \
    $(find src $OUT/gen -name '*.java') 2>&1 | quiet
test -f $OUT/classes/com/freewill/predict/PredictionKeyboard.class
javac -nowarn -cp "$TOOLS/lib/asm-7.1.jar" -d $OUT/tool "$ROOT/tools/StripParams.java"
java -cp "$OUT/tool:$TOOLS/lib/asm-7.1.jar" StripParams $(find $OUT/classes -name '*.class') 2>&1 | quiet
java -cp "$D8_CP" com.android.tools.r8.D8 --release --min-api 26 --lib "$TOOLS/android.jar" \
    --output $OUT/dex $(find $OUT/classes -name '*.class') 2>&1 | quiet

# ---- sideload APK
python3 "$ROOT/tools/pack_sign.py" $OUT/base.apk $OUT/dex/classes.dex "$ROOT/signing-key.pem" "$NAME.apk"

# ---- Play bundle
if [ ! -f "$ROOT/keystore.properties" ]; then
    echo "No keystore.properties: skipped $NAME.aab"; exit 0
fi
(cd $OUT/module && unzip -q ../proto.apk && mkdir -p manifest dex \
    && mv AndroidManifest.xml manifest/ && cp ../dex/classes.dex dex/ \
    && zip -qr ../base-module.zip manifest dex res resources.pb $( [ -d assets ] && echo assets ))
rm -f "$NAME.aab"
java -jar "$BUNDLETOOL" build-bundle --modules=$OUT/base-module.zip --output="$NAME.aab" 2>&1 | quiet
prop() { grep "^$1=" "$ROOT/keystore.properties" | cut -d= -f2-; }
jarsigner -sigalg SHA256withRSA -digestalg SHA-256 \
    -keystore "$ROOT/$(prop storeFile)" -storepass "$(prop storePassword)" -keypass "$(prop keyPassword)" \
    "$NAME.aab" "$(prop keyAlias)" 2>&1 | quiet
java -jar "$BUNDLETOOL" validate --bundle="$NAME.aab" > /dev/null 2>&1 && echo "bundled $NAME.aab $(stat -c%s "$NAME.aab") bytes"
