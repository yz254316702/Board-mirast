//DEVELOP BY PRADEEP 18/03/2021

#ifndef FBOX_NETWORK_SESSION_H_

#define FBOX_NETWORK_SESSION_H_

#include <media/stagefright/foundation/ABase.h>
#include <utils/KeyedVector.h>
#include <utils/RefBase.h>
#include <utils/Thread.h>

#include <netinet/in.h>

namespace android {

struct AMessage;
struct FBoxNetworkSession : public RefBase {
    FBoxNetworkSession();

    status_t start();

    status_t stop();

    status_t createRTSPClient(const char *host, unsigned port, const sp<AMessage> &notify,int32_t *sessionID);

    status_t createRTSPServer(const struct in_addr &addr, unsigned port,const sp<AMessage> &notify, int32_t *sessionID);

    status_t createUDPSession(unsigned localPort, const sp<AMessage> &notify, int32_t *sessionID);

    status_t createUDPSession(unsigned localPort,const char *remoteHost,unsigned remotePort,const sp<AMessage> &notify, int32_t *sessionID);

    status_t connectUDPSession(int32_t sessionID, const char *remoteHost, unsigned remotePort);

    status_t createTCPDatagramSession(const struct in_addr &addr, unsigned port,const sp<AMessage> &notify, int32_t *sessionID);

    status_t createTCPDatagramSession(unsigned localPort,const char *remoteHost,unsigned remotePort,const sp<AMessage> &notify, int32_t *sessionID);

    status_t destroySession(int32_t sessionID);

    status_t sendRequest(int32_t sessionID, const void *data, ssize_t size = -1,bool timeValid = false, int64_t timeUs = -1ll);

    void setRTPConnectionState(bool state);

    status_t switchToWebSocketMode(int32_t sessionID);

    enum NotificationReason {
        kWhatError,
        kWhatConnected,
        kWhatClientConnected,
        kWhatData,
        kWhatDatagram,
        kWhatBinaryData,
        kWhatWebSocketMessage,
        kWhatNetworkStall,
        kWhatRTPConnect,
    };

protected:
    virtual ~FBoxNetworkSession();

private:
    struct NetworkThread;
    struct Session;

    Mutex mLock;
    sp<Thread> mThread;

    int32_t mNextSessionID;

    int mPipeFd[2];

    bool mIsRTPConnection;

    KeyedVector<int32_t, sp<Session> > mSessions;

    enum Mode {
        kModeCreateUDPSession,
        kModeCreateTCPDatagramSessionPassive,
        kModeCreateTCPDatagramSessionActive,
        kModeCreateRTSPServer,
        kModeCreateRTSPClient,
    };
    status_t createClientOrServer( Mode mode, const struct in_addr *addr, unsigned port,const char *remoteHost,unsigned remotePort,const sp<AMessage> &notify,int32_t *sessionID);

    void threadLoop();
    void interrupt();

    static status_t MakeSocketNonBlocking(int s);

    DISALLOW_EVIL_CONSTRUCTORS(FBoxNetworkSession);
};

}  // namespace android

#endif  // FBOX_NETWORK_SESSION_H_
