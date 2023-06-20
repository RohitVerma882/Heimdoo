//
// Created by Rohit Verma on 02-05-2023.
//

#ifndef HEIMDOO_EXECSTRINGS_H
#define HEIMDOO_EXECSTRINGS_H

#include <jni.h>

#include "utils.h"

class ExecStrings {
public:
    ExecStrings(JNIEnv *env, jobjectArray java_string_array);

    ~ExecStrings();

    char **get();

private:
    JNIEnv *env_;
    jobjectArray java_array_;
    char **array_;

    DISALLOW_COPY_AND_ASSIGN(ExecStrings);
};

#endif //HEIMDOO_EXECSTRINGS_H
