//DEVELOP BY PRADEEP 23/03/2021
#ifndef _FBOX_UTILS_H_
#define _FBOX_UTILS_H_
#include <stdlib.h>
#include <cutils/properties.h>
#include <stdint.h>

    int32_t getPropertyInt(const char *key, int32_t def);
    void    setProperty(const char *key, const char *value);
#endif
