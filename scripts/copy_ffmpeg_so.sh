#!/usr/bin/env bash
# Copia libffmpegJNI.so desde el build de androidx/media al proyecto Roomflix.
# Uso: ./scripts/copy_ffmpeg_so.sh /ruta/al/repo/androidx/media

set -e
REPO_DIR="${1:-}"
ROOMFLIX_JNILIBS="/Users/angelj.martinez/DEV/hotelplay_launcher/app/src/main/jniLibs/armeabi-v7a"
SO_NAME="libffmpegJNI.so"

if [ -z "$REPO_DIR" ] || [ ! -d "$REPO_DIR" ]; then
  echo "Uso: $0 /ruta/al/androidx/media"
  echo "Ejemplo: $0 ~/media_roomflix_ffmpeg"
  exit 1
fi

DECODER_DIR="$REPO_DIR/libraries/decoder_ffmpeg"
# El repo media usa buildDir=buildout (gradle.properties)
BUILD_DIR="$DECODER_DIR/buildout"
BUILD_DIR_ALT="$DECODER_DIR/build"
# Rutas típicas donde Gradle/CMake dejan el .so (release y debug)
CANDIDATES=(
  "$BUILD_DIR/intermediates/cmake/release/obj/armeabi-v7a/$SO_NAME"
  "$BUILD_DIR/intermediates/cmake/debug/obj/armeabi-v7a/$SO_NAME"
  "$BUILD_DIR_ALT/intermediates/cmake/release/obj/armeabi-v7a/$SO_NAME"
  "$BUILD_DIR_ALT/intermediates/cmake/debug/obj/armeabi-v7a/$SO_NAME"
  "$BUILD_DIR/intermediates/stripped_native_libs/release/stripReleaseSymbols/out/lib/armeabi-v7a/$SO_NAME"
  "$BUILD_DIR/intermediates/merged_native_libs/release/out/lib/armeabi-v7a/$SO_NAME"
)

FOUND=""
for c in "${CANDIDATES[@]}"; do
  if [ -f "$c" ]; then
    FOUND="$c"
    break
  fi
done

if [ -z "$FOUND" ]; then
  echo "No se encontró $SO_NAME en $DECODER_DIR"
  echo "Ejecuta primero en el repo: JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :lib-decoder-ffmpeg:assembleRelease"
  exit 1
fi

mkdir -p "$ROOMFLIX_JNILIBS"
cp -v "$FOUND" "$ROOMFLIX_JNILIBS/"
echo "Listo. Recompila la app: ./gradlew assembleNormalDebug"
exit 0
