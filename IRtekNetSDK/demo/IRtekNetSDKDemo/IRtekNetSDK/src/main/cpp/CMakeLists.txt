cmake_minimum_required(VERSION 3.22.1)
project(irtek_netsdk_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(SDK_HEADER_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../library/header")
set(PAGE_SIZE_LINK_FLAG "-Wl,-z,max-page-size=16384")

if(NOT DEFINED JNI_LIBS_DIR)
    set(JNI_LIBS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs")
endif()

set(PREBUILT_SDK_LIB "${JNI_LIBS_DIR}/${ANDROID_ABI}/libirtek_netsdk.so")

add_library(irtek_netsdk SHARED IMPORTED GLOBAL)
set_target_properties(irtek_netsdk PROPERTIES
    IMPORTED_LOCATION "${PREBUILT_SDK_LIB}")

add_library(${PROJECT_NAME} SHARED irtek_netsdk_jni.cpp)

target_include_directories(${PROJECT_NAME} PRIVATE ${SDK_HEADER_DIR})
target_link_options(${PROJECT_NAME} PRIVATE ${PAGE_SIZE_LINK_FLAG})
target_link_libraries(${PROJECT_NAME} irtek_netsdk log)
