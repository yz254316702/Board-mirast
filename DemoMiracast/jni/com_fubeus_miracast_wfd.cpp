//develope by pradeep 18/02/2021

#define LOG_NDEBUG 0
#define LOG_TAG "Fbox-jni"

#include <utils/Log.h>
#include <jni.h>
#include <nativehelper/JNIHelp.h>
#include <android_runtime/AndroidRuntime.h>
#include "fboxnative/FBoxNetworkSession.h"
#include "fboxnative/WifiDisplaySink.h"
#include <media/IRemoteDisplay.h>
#include <media/IRemoteDisplayClient.h>

#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <media/IMediaPlayerService.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>


using namespace android;

sp<ALooper> mFBoxSinkLooper = new ALooper;

sp<FBoxNetworkSession> mFboxSession = new FBoxNetworkSession;
sp<WifiDisplaySink> mFBoxSink;
bool mFboxMiracastInit = false;

static android::sp<android::Surface> native_fbox_surface;

struct FBoxSinkHandler : public AHandler
{
    FBoxSinkHandler() {};
protected:
    virtual ~FBoxSinkHandler() {};
    virtual void onMessageReceived(const sp<AMessage> &msg);

private:
    enum
    {
        kWhatFBoxSinkNotify,
        kWhatFBoxSinkStopCompleted,
    };
};

sp<FBoxSinkHandler> mHandler;
static jobject FBoxSinkObject;

static void report_fbox_wfd_error(void)
{
    //JNIEnv* env = AndroidRuntime::getJNIEnv();
}

void FBoxSinkHandler::onMessageReceived(const sp<AMessage> &fbox)
{
    switch (fbox->what())
    {
        case kWhatFBoxSinkNotify:
        {
            AString reason;
            fbox->findString("reason", &reason);
            ALOGI("FBoxSinkHandler received : %s", reason.c_str());
            report_fbox_wfd_error();
            break;
        }
        case kWhatFBoxSinkStopCompleted:
        {
            ALOGI("FBoxSinkHandler received completed");
            mFBoxSinkLooper->unregisterHandler(mFBoxSink->id());
            mFBoxSinkLooper->unregisterHandler(mHandler->id());
            mFBoxSinkLooper->stop();
            mFBoxSink.clear();
            JNIEnv* fboxenv = AndroidRuntime::getJNIEnv();
            fboxenv->DeleteGlobalRef(FBoxSinkObject);
            native_fbox_surface.clear();
            mFboxMiracastInit = false;
            break;
        }
        default:
            TRESPASS();
    }
}

static android::Surface* getFBoxNativeSurface(JNIEnv* env, jobject jsurface)
{
    jclass clazz = env->FindClass("android/view/Surface");
    jfieldID field_surface;
    field_surface = env->GetFieldID(clazz, "mNativeObject", "J");
    if (field_surface == NULL) {
        ALOGE("fbox field_surface get failed");
        return NULL;
    }
    return (android::Surface *)env->GetLongField(jsurface, field_surface);
}

static int connectFBox(const char *fboxSourceHost, int32_t fboxSourcePort)
{
    ProcessState::self()->startThreadPool();
    if (!mFboxMiracastInit)
    {
        mFboxSession->start();
        if (native_fbox_surface.get()) {
            ALOGE("fbox native surface is not null we use it");
            mFBoxSink = new WifiDisplaySink(mFboxSession, native_fbox_surface.get()->getIGraphicBufferProducer());
        } else {
            ALOGE("fbox native surface is null");
            mFBoxSink = new WifiDisplaySink(mFboxSession);
        }
        mHandler = new FBoxSinkHandler();
        mFBoxSinkLooper->registerHandler(mFBoxSink);
        mFBoxSinkLooper->registerHandler(mHandler);
        mFBoxSink->setSinkHandler(mHandler);
        ALOGI("FBoxSinkHandler mFBoxSink=%d, mHandler=%d", mFBoxSink->id(), mHandler->id());
        if (fboxSourcePort >= 0) {
            mFBoxSink->start(fboxSourceHost, fboxSourcePort);
        } else {
            mFBoxSink->start(fboxSourceHost);
        }
        mFBoxSinkLooper->start(false, true, PRIORITY_DEFAULT);
        mFboxMiracastInit = true;
        ALOGI("Fbox Miracast connected");
    }
    return 0;
}

static void connect_to_fbox_wifi_source(JNIEnv *env, jclass clazz, jobject sinkobj, jobject surface, jstring jaddress, jint jfboxport)
{
   if (mFboxMiracastInit) {
        ALOGI("We should be stop WifiDisplaySink first");
        mFboxSession->stop();
        mFBoxSink->stop();
        if (native_fbox_surface.get())
            native_fbox_surface.clear();
        int times = 5;
        do {
            usleep(50000);
            times--;
            if (mFboxMiracastInit == false)
                break;
        } while(times-- > 0);
    }
    const char *address = env->GetStringUTFChars(jaddress, NULL);
    FBoxSinkObject = env->NewGlobalRef(sinkobj);
    native_fbox_surface = getFBoxNativeSurface(env, surface);
    if (android::Surface::isValid(native_fbox_surface)) {
        ALOGE("fbox_native is valid surface");
    } else {
        ALOGE("fbox_native is Invalid surface");
    }
    ALOGI("connect to wifi source %s:%d fbox_native.get() is %p", address, jfboxport, native_fbox_surface.get());
    connectFBox(address, jfboxport);
    env->ReleaseStringUTFChars(jaddress, address);
}

static void connect_to_rtsp_uri(JNIEnv *env, jclass clazz, jstring jfboxuri)
{
    const char *address = env->GetStringUTFChars(jfboxuri, NULL);
    ALOGI("connect to rtsp uri %s", address);
    connectFBox(address, -1);
    env->ReleaseStringUTFChars(jfboxuri, address);
}


static void disconnectSink(JNIEnv *env, jclass clazz)
{
    ALOGI("disconnect sink mFboxMiracastInit:%d", mFboxMiracastInit);
    if (mFboxMiracastInit == false)
        return;
    ALOGI("stop WifiDisplaySink");
    mFboxSession->stop();
    mFBoxSink->stop();
    native_fbox_surface.clear();
}

static void setTeardown(JNIEnv* env, jclass clazz)
{
    ALOGI("setTeardown");
    mFBoxSink->setTeardown();
}

static JNINativeMethod gMethods[] =
{
    {
        "nativeConnectWifiSource", "(Lcom/fubeus/miracast/FBoxSinkActivity;Landroid/view/Surface;Ljava/lang/String;I)V",
        (void *)connect_to_fbox_wifi_source
    },
    {
        "nativeDisconnectSink", "()V",
        (void *)disconnectSink
    },
    {
        "nativeSetTeardown", "()V",
        (void*) setTeardown
    },
};



jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env = NULL;
    jint result = -1;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK)
    {
        ALOGI("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);
    if (AndroidRuntime::registerNativeMethods(env, "com/fubeus/miracast/FBoxSinkActivity", gMethods, NELEM(gMethods)) < 0) {
        ALOGE("Can't register ActivityStack");
        goto bail;
    }
    result = JNI_VERSION_1_4;
bail:
    return result;
}
