# Compilar FFmpeg de Media3 para armeabi-v7a (Chromecast Google TV HD)

El dispositivo Chromecast con Google TV HD solo reporta **armeabi-v7a**. La dependencia Jellyfin `media3-ffmpeg-decoder` no incluye binarios para esta ABI. Este documento describe cómo compilar la extensión FFmpeg desde el repositorio oficial AndroidX Media y colocar el `.so` en el proyecto Roomflix.

## Requisitos

- **Android NDK** r26b (o r23c si ANDROID_ABI < 21). Ruta típica: `$ANDROID_HOME/ndk/<version>` o descarga desde [Android NDK](https://developer.android.com/ndk/downloads).
- **Git**, **CMake**, **Ninja**.
- Opcional: **Docker** si quieres un entorno aislado (ver más abajo).

## Codecs objetivo

- **mp2**, **mp2float**, **ac3**, **eac3**, **aac** (para canales MP2A/EAC3).

En el script de AndroidX Media los decoders se pasan como lista; los nombres pueden variar (ej. `mp2`, `aac`, `ac3`, `eac3`). Consulta [Supported formats (FFmpeg)](https://developer.android.com/media/media3/exoplayer/supported-formats#ffmpeg-library) para los nombres exactos.

## Pasos (sin Docker)

### 1. Clonar el repositorio fuera del proyecto

```bash
cd /tmp   # o cualquier carpeta fuera de hotelplay_launcher
git clone --depth 1 https://github.com/androidx/media.git media_roomflix_ffmpeg
cd media_roomflix_ffmpeg
```

### 2. Ruta del módulo FFmpeg y variables

```bash
FFMPEG_MODULE_PATH="$(pwd)/libraries/decoder_ffmpeg/src/main"
NDK_PATH="/ruta/a/tu/ndk"   # ej. $ANDROID_HOME/ndk/26.2.11394342
HOST_PLATFORM="darwin-x86_64"   # linux-x86_64 en Linux
ANDROID_ABI=26
ENABLED_DECODERS=(mp2 mp2float ac3 eac3 aac)
```

### 3. Clonar FFmpeg 6.0 en el jni del módulo

```bash
cd "${FFMPEG_MODULE_PATH}/jni"
git clone git://source.ffmpeg.org/ffmpeg --branch=release/6.0 --depth=1
```

### 4. Ejecutar build_ffmpeg.sh (solo armeabi-v7a)

El script por defecto compila varias ABIs. Para solo **armeabi-v7a** puedes editar `build_ffmpeg.sh` y dejar solo esa ABI en la lista, o ejecutar la parte que construye para `armeabi-v7a`:

```bash
cd "${FFMPEG_MODULE_PATH}/jni"
./build_ffmpeg.sh \
  "${FFMPEG_MODULE_PATH}" "${NDK_PATH}" "${HOST_PLATFORM}" "${ANDROID_ABI}" "${ENABLED_DECODERS[@]}"
```

Esto genera librerías estáticas (`.a`) en `ffmpeg/android-libs/armeabi-v7a/` (y otras ABIs si no se modifica el script).

### 5. Compilar el módulo decoder_ffmpeg con Gradle

Desde la raíz del repo clonado:

```bash
cd /tmp/media_roomflix_ffmpeg
./gradlew :media-lib-decoder_ffmpeg:assembleDebug
```

El `.so` estará en algo como:

`libraries/decoder_ffmpeg/build/intermediates/cmake/debug/obj/armeabi-v7a/libffmpegJNI.so`

(Ruta puede variar según versión de Android Gradle Plugin; busca `libffmpegJNI.so` bajo `decoder_ffmpeg/build`.)

### 6. Copiar el .so a Roomflix

```bash
cp libraries/decoder_ffmpeg/build/intermediates/cmake/debug/obj/armeabi-v7a/libffmpegJNI.so \
   /Users/angelj.martinez/DEV/hotelplay_launcher/app/src/main/jniLibs/armeabi-v7a/
```

Ajusta la ruta del proyecto si es distinta.

### 7. Compilar e instalar la app

En el proyecto Roomflix:

```bash
cd /Users/angelj.martinez/DEV/hotelplay_launcher
./gradlew assembleNormalDebug
adb install -r app/build/outputs/apk/normal/debug/app-normal-debug.apk
```

## Verificación

Al reproducir un canal con audio MP2A o EAC3, en Logcat (tag **RoomflixDebug**) debería aparecer algo como:

`AUDIODECODER: Usando ffmpeg.mp2` (o similar con "ffmpeg" en el nombre del decoder).

## Uso de Docker (opcional)

Si quieres usar Docker para aislar el entorno (p. ej. Linux con NDK dentro del contenedor), tendrías que crear un Dockerfile que instale NDK, clone FFmpeg y ejecute `build_ffmpeg.sh` + el paso de Gradle, y luego copiar `libffmpegJNI.so` al host. Los pasos lógicos son los mismos que arriba, ejecutados dentro del contenedor.

## Nota sobre la dependencia Jellyfin

El proyecto sigue usando **org.jellyfin.media3:media3-ffmpeg-decoder:1.3.1+2** para las **clases Java** (p. ej. `FfmpegAudioRenderer`, `FfmpegLibrary`). Esa dependencia no trae `.so` para armeabi-v7a. Al colocar **libffmpegJNI.so** en `app/src/main/jniLibs/armeabi-v7a/`, el runtime cargará ese binario y la extensión podrá decodificar en dispositivos armeabi-v7a. La configuración **abiFilters "armeabi-v7a"** en `app/build.gradle` hace que el APK solo incluya esa ABI y por tanto los `.so` de jniLibs para armeabi-v7a.
