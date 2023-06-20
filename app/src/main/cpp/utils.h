//
// Created by Rohit Verma on 02-05-2023.
//

#ifndef HEIMDOO_UTILS_H
#define HEIMDOO_UTILS_H

#include <jni.h>

#if !defined(DISALLOW_COPY_AND_ASSIGN)
#define DISALLOW_COPY_AND_ASSIGN(TypeName) \
  TypeName(const TypeName&) = delete;  \
  void operator=(const TypeName&) = delete
#endif  // !defined(DISALLOW_COPY_AND_ASSIGN)

static inline int jniThrowNullPointerException(JNIEnv *env) {
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
    }

    jclass e_class = env->FindClass("java/lang/NullPointerException");
    if (e_class == nullptr) {
        return -1;
    }

    if (env->ThrowNew(e_class, nullptr) != JNI_OK) {
        env->DeleteLocalRef(e_class);
        return -1;
    }
    env->DeleteLocalRef(e_class);
    return 0;
}

#endif //HEIMDOO_UTILS_H
