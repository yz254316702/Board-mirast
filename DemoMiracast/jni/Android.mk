LOCAL_PATH:= $(call my-dir)

# TinyPlanet
include $(CLEAR_VARS)

LOCAL_NDK_STL_VARIANT := c++_static
LOCAL_LDFLAGS   := -llog -ldl -ljnigraphics
LOCAL_SDK_VERSION := 17
LOCAL_MODULE    := libwfd_fbox_jni
LOCAL_SRC_FILES := \
        com_fubeus_miracast_wfd.cpp \
        fboxnative/FboxLinearData.cpp       \
        fboxnative/FBoxNetworkSession.cpp                \
        fboxnative/FBoxRenderData.cpp         \
        fboxnative/WifiDisplaySink.cpp        \
        fboxnative/FboxSinkData.cpp                  \
        fboxnative/FboxUtils.cpp


LOCAL_C_INCLUDES := \
    $(JNI_H_INCLUDE) \
    $(TOP)/libnativehelper/include_jni \
    $(TOP)/system/core/libutils/include \
    $(TOP)/system/core/liblog/include  \
    $(TOP)/frameworks/base/core/jni/include/ \
    $(TOP)/frameworks/av/media/libstagefright \
    $(TOP)/frameworks/native/include/media/openmax \
    $(TOP)/frameworks/av/media/libstagefright/mpeg2ts \
    $(TOP)/frameworks/native/include/media/hardware \
    $(TOP)/frameworks/native/headers/media_plugin \
    $(TOP)/frameworks/av/mediaextconfig/include

LOCAL_SHARED_LIBRARIES:= \
        libandroid_runtime\
        libnativehelper\
        libbinder \
        libgui \
        libmedia \
        libstagefright \
        libstagefright_foundation \
        liblog \
        libutils \
        libcutils \
        libui \

LOCAL_CFLAGS += -Wall -Wextra -Wno-unused-parameter
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
