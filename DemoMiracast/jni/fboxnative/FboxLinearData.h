//DEVELOP BY PRADEEP 18/03/2021

#ifndef FBOX_LINEAR_DATA_H_

#define FBOX_LINEAR_DATA_H_

#include <sys/types.h>
#include <media/stagefright/foundation/ABase.h>

namespace android
{
    struct FboxLinearData
    {
        FboxLinearData(size_t historySize);

        ~FboxLinearData();

        void addPoint(float x, float y);

        bool approxLine(float *n1, float *n2, float *b) const;

    private:
        struct Point
        {
            float mX, mY;
        };

        size_t mHistorySize;

        size_t mCount;

        Point *mHistory;

        float mSumX, mSumY;

        DISALLOW_EVIL_CONSTRUCTORS(FboxLinearData);
    };

}  // namespace android

#endif  // FBOX_LINEAR_DATA_H_
