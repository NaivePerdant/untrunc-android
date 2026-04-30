cmake_minimum_required(VERSION 3.22.1)
project(untrunc_android LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# FFmpeg pre-built libraries path
set(FFMPEG_DIR ${CMAKE_SOURCE_DIR}/ffmpeg/${ANDROID_ABI})

# FFmpeg headers
include_directories(${FFMPEG_DIR}/include)

# Import FFmpeg pre-built shared libraries
add_library(avformat SHARED IMPORTED)
set_target_properties(avformat PROPERTIES
    IMPORTED_LOCATION ${FFMPEG_DIR}/lib/libavformat.so)

add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec PROPERTIES
    IMPORTED_LOCATION ${FFMPEG_DIR}/lib/libavcodec.so)

add_library(avutil SHARED IMPORTED)
set_target_properties(avutil PROPERTIES
    IMPORTED_LOCATION ${FFMPEG_DIR}/lib/libavutil.so)

# Untrunc core sources
file(GLOB UNTRUNC_CORE_SRC
    "untrunc_core/*.cpp"
    "untrunc_core/avc1/*.cpp"
    "untrunc_core/hvc1/*.cpp"
)

# Build the shared library
add_library(untrunc_android SHARED
    jni_bridge.cpp
    ${UNTRUNC_CORE_SRC}
)

# Include untrunc_core headers
target_include_directories(untrunc_android PRIVATE
    ${CMAKE_SOURCE_DIR}/untrunc_core
    ${CMAKE_SOURCE_DIR}/untrunc_core/avc1
    ${CMAKE_SOURCE_DIR}/untrunc_core/hvc1
)

# Compile definitions
target_compile_definitions(untrunc_android PRIVATE
    _FILE_OFFSET_BITS=64
    ANDROID
)

# Link libraries
target_link_libraries(untrunc_android
    avformat
    avcodec
    avutil
    android
    log
    z
)
