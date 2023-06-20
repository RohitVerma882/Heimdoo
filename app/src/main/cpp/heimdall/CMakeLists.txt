cmake_minimum_required(VERSION 3.0.0)
project(heimdall)

#
# libusb include dirs
#
set(LIBUSB_INCLUDE_DIRS
        ../libusb/android
        ../libusb/libusb
        ../libusb
        ../libusb/libusb/os)

#
# libusb source files
#
set(LIBUSB_SOURCE_FILES
        ../libusb/libusb/core.c
        ../libusb/libusb/descriptor.c
        ../libusb/libusb/hotplug.c
        ../libusb/libusb/io.c
        ../libusb/libusb/sync.c
        ../libusb/libusb/strerror.c
        ../libusb/libusb/os/linux_usbfs.c
        ../libusb/libusb/os/events_posix.c
        ../libusb/libusb/os/threads_posix.c
        ../libusb/libusb/os/linux_netlink.c)

#
# libpit include dirs
#
set(LIBPIT_INCLUDE_DIRS
        ../libpit/source)

include_directories(${LIBPIT_INCLUDE_DIRS})

add_definitions(-DOS_LINUX)

#
# libheimdall source files
#
set(HEIMDALL_SOURCE_FILES
        source/Arguments.cpp
        source/BridgeManager.cpp
        source/ClosePcScreenAction.cpp
        source/DetectAction.cpp
        source/DownloadPitAction.cpp
        source/FlashAction.cpp
        source/HelpAction.cpp
        source/InfoAction.cpp
        source/Interface.cpp
        source/PrintPitAction.cpp
        source/Utility.cpp
        source/VersionAction.cpp)

include(CheckSymbolExists)

#
# Large file support on UN*X, a/k/a LFS.
#
include(FindLFS)
if (LFS_FOUND)
    #
    # Add the required #defines.
    #
    add_definitions(${LFS_DEFINITIONS})
endif ()

#
# Check for fseeko as well.
#
include(FindFseeko)
if (FSEEKO_FOUND)
    set(HAVE_FSEEKO ON)

    #
    # Add the required #defines.
    #
    add_definitions(${FSEEKO_DEFINITIONS})
endif ()

#
# libusb
#
add_library(usb STATIC ${LIBUSB_SOURCE_FILES})
target_compile_options(usb PRIVATE -fvisibility=hidden -pthread)
target_include_directories(usb PRIVATE ${LIBUSB_INCLUDE_DIRS})
target_include_directories(usb PUBLIC ../libusb/libusb ../libusb)
target_link_libraries(usb PRIVATE log)

#
# libheimdall
#
add_library(heimdall STATIC ${HEIMDALL_SOURCE_FILES})
target_compile_features(heimdall PRIVATE cxx_std_11)
target_include_directories(heimdall PUBLIC ${LIBPIT_INCLUDE_DIRS} source/)
target_link_libraries(heimdall PRIVATE pit usb)
