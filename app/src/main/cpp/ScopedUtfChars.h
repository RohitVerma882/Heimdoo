//
// Created by Rohit Verma on 02-05-2023.
//

#ifndef HEIMDOO_SCOPEDUTFCHARS_H
#define HEIMDOO_SCOPEDUTFCHARS_H

#include <jni.h>
#include <stddef.h>
#include <string.h>

#include "utils.h"

class ScopedUtfChars {
public:
    ScopedUtfChars(JNIEnv *env, jstring s) : env_(env), string_(s) {
        if (s == nullptr) {
            utf_chars_ = nullptr;
            jniThrowNullPointerException(env);
        } else {
            utf_chars_ = env->GetStringUTFChars(s, nullptr);
        }
    }

    ScopedUtfChars(ScopedUtfChars &&rhs) noexcept:
            env_(rhs.env_), string_(rhs.string_), utf_chars_(rhs.utf_chars_) {
        rhs.env_ = nullptr;
        rhs.string_ = nullptr;
        rhs.utf_chars_ = nullptr;
    }

    ~ScopedUtfChars() {
        if (utf_chars_) {
            env_->ReleaseStringUTFChars(string_, utf_chars_);
        }
    }

    ScopedUtfChars &operator=(ScopedUtfChars &&rhs) noexcept {
        if (this != &rhs) {
            this->~ScopedUtfChars();

            env_ = rhs.env_;
            string_ = rhs.string_;
            utf_chars_ = rhs.utf_chars_;
            rhs.env_ = nullptr;
            rhs.string_ = nullptr;
            rhs.utf_chars_ = nullptr;
        }
        return *this;
    }

    const char *c_str() const {
        return utf_chars_;
    }

    size_t size() const {
        return strlen(utf_chars_);
    }

    const char &operator[](size_t n) const {
        return utf_chars_[n];
    }

private:
    JNIEnv *env_;
    jstring string_;
    const char *utf_chars_;

    DISALLOW_COPY_AND_ASSIGN(ScopedUtfChars);
};

#endif //HEIMDOO_SCOPEDUTFCHARS_H
