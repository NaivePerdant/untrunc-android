#!/bin/bash
# Build minimal FFmpeg for Android
# Usage: ./build_ffmpeg_android.sh [ndk_path]
#
# This builds only the libraries needed by untrunc:
# libavformat, libavcodec, libavutil (shared)

set -e

FFMPEG_VERSION="6.0"
FFMPEG_URL="https://ffmpeg.org/releases/ffmpeg-${FFMPEG_VERSION}.tar.xz"
NDK=${1:-/Users/master/Library/Android/sdk/ndk/27.0.12077973}
API_LEVEL=24

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="${SCRIPT_DIR}/.ffmpeg_build"
OUTPUT_DIR="${SCRIPT_DIR}/app/src/main/cpp/ffmpeg"

# ABIs to build
ABIS="arm64-v8a armeabi-v7a x86_64"

echo "=== FFmpeg Android Build ==="
echo "NDK: ${NDK}"
echo "API: ${API_LEVEL}"
echo "Output: ${OUTPUT_DIR}"

# Download FFmpeg source
mkdir -p "${BUILD_DIR}"
if [ ! -d "${BUILD_DIR}/ffmpeg-${FFMPEG_VERSION}" ]; then
    echo ">>> Downloading FFmpeg ${FFMPEG_VERSION}..."
    cd "${BUILD_DIR}"
    if [ ! -f "ffmpeg-${FFMPEG_VERSION}.tar.xz" ]; then
        curl -L -o "ffmpeg-${FFMPEG_VERSION}.tar.xz" "${FFMPEG_URL}"
    fi
    tar xf "ffmpeg-${FFMPEG_VERSION}.tar.xz"
fi

FFMPEG_SRC="${BUILD_DIR}/ffmpeg-${FFMPEG_VERSION}"

# Toolchain
TOOLCHAIN="${NDK}/toolchains/llvm/prebuilt/darwin-x86_64"

build_ffmpeg() {
    local ABI=$1
    local ARCH
    local TARGET
    local CPU
    local EXTRA_CFLAGS=""
    local EXTRA_LDFLAGS=""

    case ${ABI} in
        arm64-v8a)
            ARCH=aarch64
            TARGET=aarch64-linux-android
            CPU=armv8-a
            ;;
        armeabi-v7a)
            ARCH=arm
            TARGET=armv7a-linux-androideabi
            CPU=armv7-a
            EXTRA_CFLAGS="-mfloat-abi=softfp -mfpu=neon"
            ;;
        x86_64)
            ARCH=x86_64
            TARGET=x86_64-linux-android
            CPU=x86-64
            ;;
    esac

    local PREFIX="${BUILD_DIR}/output/${ABI}"
    local CC="${TOOLCHAIN}/bin/${TARGET}${API_LEVEL}-clang"
    local CXX="${TOOLCHAIN}/bin/${TARGET}${API_LEVEL}-clang++"

    # Fix for armeabi-v7a: the actual binary might have a different name
    if [ "${ABI}" = "armeabi-v7a" ]; then
        CC="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang"
        CXX="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang++"
    fi

    echo ">>> Building FFmpeg for ${ABI} (${ARCH})..."

    mkdir -p "${PREFIX}"
    cd "${FFMPEG_SRC}"

    make clean 2>/dev/null || true

    ./configure \
        --prefix="${PREFIX}" \
        --enable-shared \
        --disable-static \
        --disable-doc \
        --disable-programs \
        --disable-everything \
        --enable-decoders \
        --disable-vdpau \
        --enable-demuxers \
        --enable-protocol=file \
        --disable-avdevice \
        --disable-swresample \
        --disable-swscale \
        --disable-avfilter \
        --disable-postproc \
        --disable-vulkan \
        --disable-v4l2-m2m \
        --disable-vaapi \
        --disable-vdpau \
        --disable-videotoolbox \
        --disable-audiotoolbox \
        --enable-cross-compile \
        --target-os=android \
        --arch=${ARCH} \
        --cpu=${CPU} \
        --cc="${CC}" \
        --cxx="${CXX}" \
        --cross-prefix="${TOOLCHAIN}/bin/llvm-" \
        --nm="${TOOLCHAIN}/bin/llvm-nm" \
        --ar="${TOOLCHAIN}/bin/llvm-ar" \
        --ranlib="${TOOLCHAIN}/bin/llvm-ranlib" \
        --strip="${TOOLCHAIN}/bin/llvm-strip" \
        --sysroot="${TOOLCHAIN}/sysroot" \
        --extra-cflags="-Os -fPIC ${EXTRA_CFLAGS}" \
        --extra-ldflags="${EXTRA_LDFLAGS}" \
        --disable-x86asm \
        --pkg-config=/dev/null

    make -j$(sysctl -n hw.ncpu) 2>&1 | tail -5
    make install

    echo ">>> Installed FFmpeg for ${ABI} to ${PREFIX}"
}

# Build for each ABI
for ABI in ${ABIS}; do
    build_ffmpeg ${ABI}
done

# Copy results to project
echo ">>> Copying to project..."
for ABI in ${ABIS}; do
    local_prefix="${BUILD_DIR}/output/${ABI}"
    target="${OUTPUT_DIR}/${ABI}"
    mkdir -p "${target}/include" "${target}/lib"

    # Copy headers (same for all ABIs, take from first)
    cp -R "${local_prefix}/include/libavformat" "${target}/include/"
    cp -R "${local_prefix}/include/libavcodec" "${target}/include/"
    cp -R "${local_prefix}/include/libavutil" "${target}/include/"

    # Copy shared libraries
    cp "${local_prefix}/lib/libavformat.so"* "${target}/lib/" 2>/dev/null || true
    cp "${local_prefix}/lib/libavcodec.so"* "${target}/lib/" 2>/dev/null || true
    cp "${local_prefix}/lib/libavutil.so"* "${target}/lib/" 2>/dev/null || true

    # Also copy versioned .so to unversioned names if needed
    for lib in avformat avcodec avutil; do
        if [ ! -f "${target}/lib/lib${lib}.so" ]; then
            # Find the versioned .so and create a symlink
            versioned=$(ls "${target}/lib/lib${lib}.so."* 2>/dev/null | head -1)
            if [ -n "${versioned}" ]; then
                cp "${versioned}" "${target}/lib/lib${lib}.so"
            fi
        fi
    done

    echo "  ${ABI}: done"
done

echo "=== FFmpeg Android Build Complete ==="
echo "Headers and libraries installed to: ${OUTPUT_DIR}"
