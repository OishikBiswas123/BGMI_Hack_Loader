cmake_minimum_required(VERSION 3.22)
project("bgmi_native")

add_library(
    bgmi_native
    SHARED
    native-lib.cpp
)

target_include_directories(bgmi_native PRIVATE .)

target_link_libraries(
    bgmi_native
    log
)
