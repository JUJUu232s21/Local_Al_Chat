cmake_minimum_required(VERSION 3.22.1)

project("local_al_chat" LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED true)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED true)

set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "build examples" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "build tests" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "build server" FORCE)

if(DEFINED ANDROID_ABI)
    if(ANDROID_ABI STREQUAL "arm64-v8a")
        set(GGML_SYSTEM_ARCH "ARM")
        set(GGML_CPU_KLEIDIAI ON CACHE BOOL "enable KleidiAI" FORCE)
        set(GGML_OPENMP ON CACHE BOOL "enable OpenMP" FORCE)
    elseif(ANDROID_ABI STREQUAL "x86_64")
        set(GGML_SYSTEM_ARCH "x86")
        set(GGML_CPU_KLEIDIAI OFF CACHE BOOL "disable KleidiAI" FORCE)
        set(GGML_OPENMP OFF CACHE BOOL "disable OpenMP" FORCE)
    endif()
endif()

add_subdirectory(llama.cpp build-llama)

add_library(${CMAKE_PROJECT_NAME} SHARED
    local_al_chat.cpp)

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
    llama.cpp
    llama.cpp/common
    llama.cpp/include
    llama.cpp/ggml/include
    llama.cpp/ggml/src)

target_link_libraries(${CMAKE_PROJECT_NAME}
    llama
    ggml
    android
    log)
