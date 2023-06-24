//
// Created by Rohit Verma on 02-05-2023.
//

#include <jni.h>
#include <cstdio>

#include "Heimdoo.h"
#include "ScopedUtfChars.h"
#include "ExecStrings.h"

using namespace heimdoo;

extern "C"
JNIEXPORT void JNICALL
Java_dev_rohitverma882_heimdoo_Heimdoo_execHeimdall(JNIEnv *env, jclass thiz,
                                                    jstring
                                                    jstd_out,
                                                    jstring jstd_err, jint
                                                    fd,
                                                    jobjectArray jargs) {
    ScopedUtfChars std_out(env, jstd_out);
    ScopedUtfChars std_err(env, jstd_err);
    freopen(std_out.c_str(), "w", stdout);
    freopen(std_err.c_str(), "w", stderr);
    ExecStrings args(env, jargs);
    exec_heimdall(fd, env->GetArrayLength(jargs), args.get());
    fclose(stdout);
    fclose(stderr);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_dev_rohitverma882_heimdoo_Heimdoo_isHeimdallDevice(JNIEnv *env, jclass clazz, jint fd) {
    bool result = is_heimdall_device(fd);
    return (result ? JNI_TRUE : JNI_FALSE);
}

extern "C"
JNIEXPORT void JNICALL
Java_dev_rohitverma882_heimdoo_Heimdoo_init(JNIEnv *env, jobject thiz) {

}