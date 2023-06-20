//
// Created by Rohit Verma on 02-05-2023.
//

#ifndef HEIMDOO_SCOPEDLOCALREF_H
#define HEIMDOO_SCOPEDLOCALREF_H

#include <jni.h>
#include <cstddef>

#include "utils.h"

template<typename T>
class ScopedLocalRef {
public:
    ScopedLocalRef(JNIEnv *env, T localRef) : mEnv(env), mLocalRef(localRef) {
    }

    ScopedLocalRef(ScopedLocalRef &&s)

    noexcept : mEnv(s.mEnv), mLocalRef(s
    .

    release()

    ) {
    }

    explicit ScopedLocalRef(JNIEnv *env) : mEnv(env), mLocalRef(nullptr) {
    }

    ~ScopedLocalRef() {
        reset();
    }

    void reset(T ptr = nullptr) {
        if (ptr != mLocalRef) {
            if (mLocalRef != nullptr) {
                mEnv->DeleteLocalRef(mLocalRef);
            }
            mLocalRef = ptr;
        }
    }

    T release() __attribute__((warn_unused_result)) {
        T localRef = mLocalRef;
        mLocalRef = nullptr;
        return localRef;
    }

    T get() const {
        return mLocalRef;
    }

    ScopedLocalRef &operator=(ScopedLocalRef &&s)

    noexcept {
        reset(s.release());
        mEnv = s.mEnv;
        return *this;
    }

    bool operator==(std::nullptr_t) const {
        return mLocalRef == nullptr;
    }

    bool operator!=(std::nullptr_t) const {
        return mLocalRef != nullptr;
    }

private:
    JNIEnv *mEnv;
    T mLocalRef;

    DISALLOW_COPY_AND_ASSIGN(ScopedLocalRef);
};

#endif //HEIMDOO_SCOPEDLOCALREF_H
