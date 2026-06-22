#include <jni.h>
#include <android/log.h>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <cstdlib>
#include <unistd.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <vector>
#include <string>

#define LOG_TAG "BgmiNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static pid_t target_pid = 0;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_cheatlearn_bgmiloader_utils_NativeReader_attachProcess(JNIEnv *env, jclass clazz, jint pid) {
    target_pid = (pid_t)pid;
    LOGI("Attached to PID: %d", target_pid);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_cheatlearn_bgmiloader_utils_NativeReader_getMyPid(JNIEnv *env, jclass clazz) {
    return (jint)getpid();
}

static bool read_memory(uintptr_t addr, void *buf, size_t size) {
    if (target_pid == 0) return false;
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/mem", target_pid);
    int fd = open(path, O_RDONLY);
    if (fd < 0) return false;
    lseek(fd, (off_t)addr, SEEK_SET);
    bool ok = (read(fd, buf, size) == (ssize_t)size);
    close(fd);
    return ok;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_cheatlearn_bgmiloader_utils_NativeReader_readLong(JNIEnv *env, jclass clazz, jlong addr) {
    uint64_t val = 0;
    if (read_memory((uintptr_t)addr, &val, sizeof(val))) return (jlong)val;
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_cheatlearn_bgmiloader_utils_NativeReader_readInt(JNIEnv *env, jclass clazz, jlong addr) {
    uint32_t val = 0;
    if (read_memory((uintptr_t)addr, &val, sizeof(val))) return (jint)val;
    return 0;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_cheatlearn_bgmiloader_utils_NativeReader_readFloat(JNIEnv *env, jclass clazz, jlong addr) {
    float val = 0.0f;
    if (read_memory((uintptr_t)addr, &val, sizeof(val))) return (jfloat)val;
    return 0.0f;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_cheatlearn_bgmiloader_utils_NativeReader_readBuffer(JNIEnv *env, jclass clazz,
                                                              jlong addr, jbyteArray buffer) {
    jsize len = env->GetArrayLength(buffer);
    jbyte *bytes = env->GetByteArrayElements(buffer, nullptr);
    if (!bytes) return -1;
    bool ok = read_memory((uintptr_t)addr, bytes, (size_t)len);
    env->ReleaseByteArrayElements(buffer, bytes, 0);
    return ok ? (jint)len : -1;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_cheatlearn_bgmiloader_utils_NativeReader_readString(JNIEnv *env, jclass clazz,
                                                              jlong addr, jint maxLen) {
    char buf[256];
    int len = maxLen > 255 ? 255 : maxLen;
    if (!read_memory((uintptr_t)addr, buf, (size_t)len)) {
        return env->NewStringUTF("");
    }
    buf[len - 1] = '\0';
    // Find actual end (null terminator)
    for (int i = 0; i < len - 1; i++) {
        if (buf[i] == '\0' || buf[i] < 32) {
            buf[i] = '\0';
            break;
        }
    }
    return env->NewStringUTF(buf);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_cheatlearn_bgmiloader_utils_NativeReader_getModuleBase(JNIEnv *env, jclass clazz,
                                                                 jint pid, jstring moduleName) {
    const char *name = env->GetStringUTFChars(moduleName, nullptr);
    if (!name) return 0;

    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/maps", (pid_t)pid);
    FILE *fp = fopen(path, "r");
    if (!fp) {
        env->ReleaseStringUTFChars(moduleName, name);
        return 0;
    }

    uintptr_t base = 0;
    char line[1024];
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, name) && strstr(line, "r-xp")) {
            base = strtoul(line, nullptr, 16);
            break;
        }
    }
    fclose(fp);
    env->ReleaseStringUTFChars(moduleName, name);
    LOGI("Module base for %s: 0x%lX", name, base);
    return (jlong)base;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGI("Native library loaded");
    return JNI_VERSION_1_6;
}
