//DEVELOP BY PRADEEP 21/03/2021

#ifndef FBOX_RENDER_DATA_H_

#define FBOX_RENDER_DATA_H_

#include <gui/Surface.h>
#include <media/stagefright/foundation/AHandler.h>
#include <string>

using namespace std;

namespace android
{
    struct ABuffer;
    class SurfaceComposerClient;
    class SurfaceControl;
    class Surface;
    class IMediaPlayer;
    struct IStreamListener;
    struct FBoxRenderData : public AHandler
    {
        FBoxRenderData(
            const sp<AMessage> &notifyLost,
            const sp<IGraphicBufferProducer> &bufferProducer,
            const sp<AMessage> &msgNotify);

        sp<ABuffer> dequeueBuffer();

        enum
        {
            kWhatQueueBuffer,
        };

        enum {
            kWhatNoPacketMsg,
            kWahtLostPacketMsg,
            kWhatConnectedNoPacketCheck,
        };

        void setIsHDCP(bool isHDCP);
        bool getIsHDCP() { return mIsHDCP; }
    protected:
        virtual void onMessageReceived(const sp<AMessage> &msg);
        virtual ~FBoxRenderData();

    private:
        struct PlayerClient;
        struct StreamSource;

        mutable Mutex mLock;

        sp<AMessage> mNotifyLost;
        sp<IGraphicBufferProducer> mBufferProducer;

        List<sp<ABuffer> > mPackets;
        int64_t mTotalBytesQueued;
        int64_t mMaxBytesQueued;
        int64_t mMinBytesQueued;
        int64_t mRetryTimes;
        int64_t mBandwidth;
        int64_t mCurTime;
        int32_t mBytesQueued;
        bool    mDebugEnable;

        sp<SurfaceComposerClient> mComposerClient;
        sp<SurfaceControl> mSurfaceControl;
        sp<Surface> mSurface;
        sp<PlayerClient> mPlayerClient;
        sp<IMediaPlayer> mPlayer;
        sp<StreamSource> mStreamSource;

        int32_t mLastDequeuedExtSeqNo;
        int64_t mFirstFailedAttemptUs;
        int32_t mPackageSuccess;
        int32_t mPackageFailed;
        int32_t mPackageRequest;
        bool mRequestedRetry;
        bool mRequestedRetransmission;
        sp<AMessage> mMsgNotify;
        bool mIsHDCP;

        void initPlayer();
        void destroyPlayer();

        void queueBuffer(const sp<ABuffer> &buffer);
        bool mIsDestoryState;
        DISALLOW_EVIL_CONSTRUCTORS(FBoxRenderData);
    };

}  // namespace android

#endif  // FBOX_RENDER_DATA_H_
