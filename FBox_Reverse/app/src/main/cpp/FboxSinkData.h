//DEVELOP BY PRADEEP 20/03/2021

#ifndef FBOX_SINK_DATA_H_

#define FBOX_SINK_DATA_H_

#include <media/stagefright/foundation/AHandler.h>
#include "FboxLinearData.h"
#include <gui/Surface.h>


namespace android
{

    struct ABuffer;
    struct FBoxNetworkSession;
    struct FBoxRenderData;
    struct FboxSinkData : public AHandler
    {
        FboxSinkData(const sp<FBoxNetworkSession> &netSession,
                const sp<IGraphicBufferProducer> &bufferProducer,
                const sp<AMessage> &msgNotify);
        status_t init(bool useTCPInterleaving);

        status_t connect(
            const char *host, int32_t remoteRtpPort, int32_t remoteRtcpPort);

        int32_t getRTPPort() const;

        status_t injectPacket(bool isRTP, const sp<ABuffer> &buffer);

        void setIsHDCP(bool isHDCP);

    protected:
        virtual void onMessageReceived(const sp<AMessage> &msg);
        virtual ~FboxSinkData();

    private:
        enum
        {
            kWhatRTPNotify,
            kWhatRTCPNotify,
            kWhatSendRR,
            kWhatPacketLost,
            kWhatInject,
        };

        struct Source;
        struct StreamSource;

        sp<FBoxNetworkSession> mNetSession;
        sp<IGraphicBufferProducer> mBufferProducer;
        KeyedVector<uint32_t, sp<Source> > mSources;

        int32_t mRTPPort;
        int32_t mRTPSessionID;
        int32_t mRTCPSessionID;

        int64_t mFirstArrivalTimeUs;
        int64_t mNumPacketsReceived;
        FboxLinearData mRegression;
        int64_t mMaxDelayMs;
        sp<AMessage> mMsgNotify;

        sp<FBoxRenderData> mRenderer;
        int32_t mDumpEnable;

        bool mIsHDCP;

        status_t parseRTP(const sp<ABuffer> &buffer);
        status_t parseRTCP(const sp<ABuffer> &buffer);
        status_t parseBYE(const uint8_t *data, size_t size);
        status_t parseSR(const uint8_t *data, size_t size);

        void addSDES(const sp<ABuffer> &buffer);
        void onSendRR();
        void onPacketLost(const sp<AMessage> &msg);
        void scheduleSendRR();

        DISALLOW_EVIL_CONSTRUCTORS(FboxSinkData);
    };

}  // namespace android

#endif  // RTP_SINK_H_
