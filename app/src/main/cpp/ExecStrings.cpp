//
// Created by Rohit Verma on 02-05-2023.
//

#include "ExecStrings.h"

#include <cstdlib>

#include "ScopedLocalRef.h"

ExecStrings::ExecStrings(JNIEnv *env, jobjectArray java_string_array)
        : env_(env), java_array_(java_string_array), array_(nullptr) {
    if (java_array_ == nullptr) {
        return;
    }

    jsize length = env_->GetArrayLength(java_array_);
    array_ = new char *[length];
    for (jsize i = 0; i < length; ++i) {
        ScopedLocalRef<jstring> java_string(env_,
                                            reinterpret_cast<jstring>(env_->GetObjectArrayElement(
                                                    java_array_, i)));
        char *string = const_cast<char *>(env_->GetStringUTFChars(java_string.get(), nullptr));
        array_[i] = string;
    }
}

ExecStrings::~ExecStrings() {
    if (array_ == nullptr) {
        return;
    }

    jthrowable pending_exception = env_->ExceptionOccurred();
    if (pending_exception != nullptr) {
        env_->ExceptionClear();
    }

    jsize length = env_->GetArrayLength(java_array_);
    for (jsize i = 0; i < length; ++i) {
        ScopedLocalRef<jstring> java_string(env_,
                                            reinterpret_cast<jstring>(env_->GetObjectArrayElement(
                                                    java_array_, i)));
        env_->ReleaseStringUTFChars(java_string.get(), array_[i]);
    }
    delete[] array_;

    if (pending_exception != nullptr) {
        if (env_->Throw(pending_exception) < 0) {
            // LOGE("Error rethrowing exception!");
        }
    }
}

char **ExecStrings::get() {
    return array_;
}
