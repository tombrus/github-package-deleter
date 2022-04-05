#!/bin/bash -ue

    JAVA_VERSION="18"
    MAIN_VERSION="1.0.0"
       MAIN_NAME="github-package-deleter"
      MAIN_CLASS="com.tombrus.githubPackageDeleter.GithubPackageDeleter"
        LIB_JARS=("lib/mvg-json-1.6.3.jar")
   EXTRA_MODULES="jdk.crypto.ec,jdk.localedata"
   MAIN_ICON_PNG="$MAIN_NAME.png"
 MAIN_CLASS_FILE="out/production/$MAIN_NAME/${MAIN_CLASS//.//}.class"
        MAIN_JAR="$MAIN_NAME.jar"
        JARS_DIR="out/artifacts/$MAIN_NAME-jar"
   TMP_INSTALLER="out/makeMacApp"
        TMP_LIBS="$TMP_INSTALLER/input/libs"
     TMP_RUNTIME="$TMP_INSTALLER/java-runtime"
    TMP_ICON_DIR="$TMP_INSTALLER/$MAIN_NAME.iconset"
       MAIN_ICNS="$TMP_INSTALLER/$MAIN_NAME.icns"

export JAVA_HOME="$(/usr/libexec/java_home -v $JAVA_VERSION)"

echo "########## prepare..."
rm -rfd "$TMP_INSTALLER"
mkdir -p "$TMP_LIBS" "$TMP_ICON_DIR"
cp "$JARS_DIR/$MAIN_JAR" "${LIB_JARS[@]}" "$TMP_LIBS"

echo "########## detecting required modules..."
detected_modules="$(
    "$JAVA_HOME/bin/jdeps" \
        -q \
        --multi-release             "$JAVA_VERSION" \
        --ignore-missing-deps \
        --print-module-deps \
        --class-path                "$TMP_LIBS"/*.jar "$MAIN_CLASS_FILE"
)"
echo "           detected modules:"
echo "$detected_modules" | tr ',' '\n' | sort | sed 's/^/               /'

echo "########## creating java runtime image..."
"$JAVA_HOME/bin/jlink" \
        --strip-native-commands \
        --no-header-files \
        --no-man-pages  \
        --compress="2"  \
        --strip-debug \
        --add-modules               "$EXTRA_MODULES,$detected_modules" \
        --include-locales="en,nl" \
        --output                    "$TMP_RUNTIME"

echo "########## creating icon set..."
sips -z   16   16 "$MAIN_ICON_PNG" --out "$TMP_ICON_DIR/icon_16x16.png"
sips -z   32   32 "$MAIN_ICON_PNG" --out "$TMP_ICON_DIR/icon_16x16@2x.png"
sips -z   32   32 "$MAIN_ICON_PNG" --out "$TMP_ICON_DIR/icon_32x32.png"
sips -z   64   64 "$MAIN_ICON_PNG" --out "$TMP_ICON_DIR/icon_32x32@2x.png"
sips -z  128  128 "$MAIN_ICON_PNG" --out "$TMP_ICON_DIR/icon_128x128.png"
sips -z  256  256 "$MAIN_ICON_PNG" --out "$TMP_ICON_DIR/icon_128x128@2x.png"
sips -z  256  256 "$MAIN_ICON_PNG" --out "$TMP_ICON_DIR/icon_256x256.png"
sips -z  512  512 "$MAIN_ICON_PNG" --out "$TMP_ICON_DIR/icon_256x256@2x.png"
sips -z  512  512 "$MAIN_ICON_PNG" --out "$TMP_ICON_DIR/icon_512x512.png"
sips -z 1024 1024 "$MAIN_ICON_PNG" --out "$TMP_ICON_DIR/icon_512x512@2x.png"
iconutil -c icns "$TMP_ICON_DIR"

echo "########## creating application..."
"$JAVA_HOME/bin/jpackage" \
        --type                      "app-image" \
        --dest                      "$TMP_INSTALLER" \
        --input                     "$TMP_LIBS" \
        --name                      "$MAIN_NAME" \
        --main-class                "$MAIN_CLASS" \
        --main-jar                  "$MAIN_JAR" \
        --java-options              "-Xmx2048m" \
        --runtime-image             "$TMP_RUNTIME" \
        --icon                      "$MAIN_ICNS" \
        --app-version               "$MAIN_VERSION" \
        --vendor                    "Tom Brus" \
        --copyright                 "Copyright ©2022 Tom Brus" \
        --mac-package-identifier    "nl.tombrus.$MAIN_NAME.app" \
        --mac-package-name          "$MAIN_NAME"
echo "           generated app: $PWD/$TMP_INSTALLER/$MAIN_NAME.app"

echo "########## install on desktop..."
rm -rf ~/"Desktop/$MAIN_NAME.app"
cp -r "$TMP_INSTALLER/$MAIN_NAME.app" ~/"Desktop"

echo "########## done"
