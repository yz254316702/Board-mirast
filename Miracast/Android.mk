LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4

LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-appcompat

LOCAL_STATIC_JAVA_LIBRARIES += android-support-design

LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-recyclerview

LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-cardview

LOCAL_STATIC_JAVA_LIBRARIES += glide

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/appcompat/res

LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/design/res 

LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/cardview/res

#LOCAL_SDK_VERSION := current

LOCAL_CERTIFICATE := platform

LOCAL_PRIVILEGED_MODULE := true

LOCAL_PACKAGE_NAME := FBoxMiracast

LOCAL_AAPT_FLAGS := --auto-add-overlay

LOCAL_AAPT_FLAGS += --extra-packages android.support.design

LOCAL_AAPT_FLAGS += --extra-packages android.support.v4

LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.appcompat

LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.cardview

#LOCAL_JNI_SHARED_LIBRARIES := libfboxnative_ha
include $(BUILD_PACKAGE)


