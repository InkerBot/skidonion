cmake_minimum_required(VERSION 3.26)
project(phantomshield_cpputils)

set(CMAKE_CXX_STANDARD 17)

enable_language( ASM_MASM )

file(GLOB_RECURSE LIBS **.lib)
file(GLOB_RECURSE INCLUDES **.h **.hpp)
file(GLOB_RECURSE SOURCES
        **.cpp
        library/**.cpp
        **.asm
        library/**.asm
)

add_library(${PROJECT_NAME} SHARED
        ${SOURCES}
        ${INCLUDES}
)

target_link_libraries(
        ${PROJECT_NAME}
        ${LIBS}
)
