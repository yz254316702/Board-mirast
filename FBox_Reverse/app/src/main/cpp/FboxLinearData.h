//DEVELOP BY PRADEEP 18/03/2021

#ifndef FBOX_LINEAR_DATA_H_

#define FBOX_LINEAR_DATA_H_
#include "../../../../../../Android/Sdk/ndk/21.1.6352462/toolchains/llvm/prebuilt/linux-x86_64/lib64/clang/9.0.8/include/opencl-c-base.h"

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
    };

}  // namespace android

#endif  // FBOX_LINEAR_DATA_H_
