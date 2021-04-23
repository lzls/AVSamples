#include <jni.h>
#include <cstdlib>
#include "android/bitmap.h"

extern "C" {
#include "gif_lib.h"

#define abgr(a, b, g, r) (((a) & 0xff) << 24) | (((b) & 0xff) << 16) | (((g) & 0xff) << 8) | ((r) & 0xff)

typedef struct GifBean {
    int currentFrame;
//    int frameCount;
} GifBean;

JNIEXPORT jlong JNICALL
Java_com_liuzhenlin_gifloader_GifLoader_load(JNIEnv *env, jclass clazz, jstring path) {
    const char *nPath = env->GetStringUTFChars(path, 0);
    int error;
    GifFileType *gifFileType = DGifOpenFileName(nPath, &error);
    if (!error) {
        DGifSlurp(gifFileType);
        GifBean *gifBean = (GifBean *) calloc(1, sizeof(GifBean));
        gifBean->currentFrame = 0;
//        gifBean->frameCount = gifFileType->ImageCount;
        gifFileType->UserData = gifBean;
    }
    env->ReleaseStringUTFChars(path, nPath);
    return (jlong) gifFileType;
}

JNIEXPORT jint JNICALL
Java_com_liuzhenlin_gifloader_GifLoader_getGifWidth(JNIEnv *env, jclass clazz,
                                                    jlong native_gif_loader) {
    GifFileType *gifFileType = (GifFileType *) native_gif_loader;
    return gifFileType->SWidth;
}

JNIEXPORT jint JNICALL
Java_com_liuzhenlin_gifloader_GifLoader_getGifHeight(JNIEnv *env, jclass clazz,
                                                     jlong native_gif_loader) {
    GifFileType *gifFileType = (GifFileType *) native_gif_loader;
    return gifFileType->SHeight;
}

static void drawFrame(GifFileType *gifFileType, AndroidBitmapInfo *frameInfo, int **pixels) {
    GifBean *gifBean = (GifBean *) gifFileType->UserData;
    SavedImage savedImage = gifFileType->SavedImages[gifBean->currentFrame];
    GifImageDesc imageDesc = savedImage.ImageDesc;
    GifColorType *colors = imageDesc.ColorMap->Colors;
    int *line = *pixels;
    int pixelIndex;
    GifByteType colorIndex;
    for (int y = imageDesc.Top; y < imageDesc.Top + imageDesc.Height; y++) {
        for (int x = imageDesc.Left; x < imageDesc.Left + imageDesc.Width; x++) {
            pixelIndex = (y - imageDesc.Top) * imageDesc.Width + (x - imageDesc.Left);
            colorIndex = savedImage.RasterBits[pixelIndex];
            GifColorType gifColorType = colors[colorIndex];
            line[x - imageDesc.Left] =
                    abgr(255, gifColorType.Blue, gifColorType.Green, gifColorType.Red);
        }
        line = (int *) ((char *) line + frameInfo->stride);
    }
}

JNIEXPORT jint JNICALL
Java_com_liuzhenlin_gifloader_GifLoader_updateFrame(JNIEnv *env, jclass clazz,
                                                    jlong native_gif_loader, jobject bmp) {
    GifFileType *gifFileType = (GifFileType *) native_gif_loader;
    GifBean *gifBean = (GifBean *) gifFileType->UserData;

    AndroidBitmapInfo bmpInfo;
    AndroidBitmap_getInfo(env, bmp, &bmpInfo);
    int *pixels;
    AndroidBitmap_lockPixels(env, bmp, (void **) &pixels);
    drawFrame(gifFileType, &bmpInfo, &pixels);
    AndroidBitmap_unlockPixels(env, bmp);

    int currentFrame = gifBean->currentFrame;
    gifBean->currentFrame++;
    if (gifBean->currentFrame >= gifFileType->ImageCount /*gifBean->frameCount*/) {
        gifBean->currentFrame = 0;
    }

    GraphicsControlBlock gcb;
    DGifSavedExtensionToGCB(gifFileType, currentFrame, &gcb);
    return gcb.DelayTime * 10;
}

JNIEXPORT void JNICALL
Java_com_liuzhenlin_gifloader_GifLoader_release(JNIEnv *env, jclass clazz,
                                                jlong native_gif_loader) {
    GifFileType *gifFileType = (GifFileType *) native_gif_loader;
    DGifCloseFile(gifFileType, NULL);
}
}
