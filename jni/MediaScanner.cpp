#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <dirent.h>
#include <sys/stat.h>

#include "Debug.h"

class MediaScannerCallback {
public:
	MediaScannerCallback(JNIEnv *env, jobject obj)
		: mEnv(env), mObj(env->NewGlobalRef(obj)), mScanFileMethodID(0) {
		jclass clz = mEnv->GetObjectClass(obj);
		mScanFileMethodID = env->GetMethodID(clz, "handleScanFileFromNative", "(ILjava/lang/String;)V");
		env->DeleteLocalRef(clz);
	}

	void handleScanFileFromNative(int type, const char *pPath) {
		jstring pathStr;
		if ((pathStr = mEnv->NewStringUTF(pPath)) == NULL) {
			mEnv->ExceptionClear();
			return;
		}

		mEnv->CallVoidMethod(mObj, mScanFileMethodID, type, pathStr);
		mEnv->DeleteLocalRef(pathStr);
	}

	~MediaScannerCallback() {
		mEnv->DeleteGlobalRef(mObj);
	}

private:
    JNIEnv *mEnv;
    jobject mObj;
    jmethodID mScanFileMethodID;
};


// 媒体类型定义
#define  MEDIA_TYPE_ERR		-1
#define  MEDIA_TYPE_AUDIO	1
#define  MEDIA_TYPE_VIDEO	2
#define  MEDIA_TYPE_IMAGE	3

// 文件路径最大长度
#define MAX_PATH	260

/**
 * 是否是不扫描的文件夹
 */
static bool isIgnoredFolder(const char *pFolder) {
	// .开头的文件夹为隐藏文件夹，不扫描
	if (pFolder[0] == '.') {
		return true;
	}

	// 不扫描的文件夹
	static const char *sIgnoredFolderTab[] = {
		"LOST.DIR", "$RECYCLE.BIN", "cacheData", "cache", "adcache",
		"VideoCache", "Lyric", "tencent", "log", "IssueReporter",
		"autonavi", "autonavidata", "NaviOne", "IGO"
	};

	static const int N = sizeof(sIgnoredFolderTab) / sizeof(sIgnoredFolderTab[0]);
	for (int i = 0; i < N; ++i) {
		if (strcasecmp(pFolder, sIgnoredFolderTab[i]) == 0) {
			return true;
		}
	}

	return false;
}

/**
 * 获取文件后缀名
 */
static const char* getFileExtName(const char *pFile) {
	// 查找'.'最后出现的位置
	const char *p = strrchr(pFile, '.');
	return (p != NULL) ? p + 1 : NULL;
}

/**
 * 匹配后缀名
 */
static bool isMatchExtName(const char **pExtNameTab, int lenOfTab, const char *pExtName) {
	for (int i = 0; i < lenOfTab; ++i) {
		if (strcasecmp(pExtName, pExtNameTab[i]) == 0) {
			return true;
		}
	}

	return false;
}

static int getMediaType(const char *pFile) {
	if (pFile == NULL) {
		return MEDIA_TYPE_ERR;
	}

	const char *pExtName = getFileExtName(pFile);
	if (pExtName != NULL) {
		static const char *sAudioExtNameTab[] = {
			"mp3", "wma", "wav", "aac", "ma4", "flac",
	        "ape", "ogg", "ra", "m4a", "m4r", "mp2"
		};
		static const int AUDIO_TAB_LEN = sizeof(sAudioExtNameTab) / sizeof(sAudioExtNameTab[0]);

		static const char *sVideoExtNameTab[] = {
			"mp4", "avi", "wmv", "flv", "ts", "m2ts",
			"tp", "mov", "vob", "mkv", "3gp", "3ga",
			"asf", "divx", "mpg", "mpeg", "m4v", "m2v"
		};
		static const int VIDEO_TAB_LEN = sizeof(sVideoExtNameTab) / sizeof(sVideoExtNameTab[0]);

		static const char *sImageExtNameTab[] = {
			"png", "jpg", "jpeg", "bmp", "gif"
		};
		static const int IMAGE_TAB_LEN = sizeof(sImageExtNameTab) / sizeof(sImageExtNameTab[0]);

		if (isMatchExtName(sAudioExtNameTab, AUDIO_TAB_LEN, pExtName)) {
			return MEDIA_TYPE_AUDIO;
		}

		if (isMatchExtName(sVideoExtNameTab, VIDEO_TAB_LEN, pExtName)) {
			return MEDIA_TYPE_VIDEO;
		}

		if (isMatchExtName(sImageExtNameTab, IMAGE_TAB_LEN, pExtName)) {
			return MEDIA_TYPE_IMAGE;
		}
	}

	return MEDIA_TYPE_ERR;
}

/**
 * 递归扫描
 */
static void doScanDir(const char *pDir, MediaScannerCallback &mscb) {
	DIR *dp;
	struct dirent *entry;
	struct stat buff;

	if ((dp = opendir(pDir)) != NULL) {
		int mediaType;
		char *pFile = new char[MAX_PATH];	// 递归调用，所以这里在堆中申请内存，防止栈溢出
		strcpy(pFile, pDir);
		int dirLen = strlen(pDir);

		pFile[dirLen++] = '/';
		pFile[dirLen] = '\0';

		// 指向开始改动的位置
		char *pFileName = pFile + dirLen;

		while ((entry = readdir(dp)) != NULL) {
			if ((dirLen + strlen(entry->d_name)) >= MAX_PATH) {
				// 路径长度超过260则不处理该路径的文件
				continue;
			}

			strcpy(pFileName, entry->d_name);
			if (stat(pFile, &buff)) {
				LOGICTRL(UART_DEBUG, "doScanDir pFile: %s, stat error!", pFile);
				continue;
			}

			if (S_ISREG(buff.st_mode)) {	// 文件
				mediaType = getMediaType(pFile);
				if (mediaType != MEDIA_TYPE_ERR) {
					mscb.handleScanFileFromNative(mediaType, pFile);
				}
			} else if (S_ISDIR(buff.st_mode)) {	// 目录
				if ((strcmp(".", entry->d_name) != 0) && (strcmp("..", entry->d_name) != 0)) {
					if (!isIgnoredFolder(entry->d_name))	{
						doScanDir(pFile, mscb);
					} else {
						LOGICTRL(UART_DEBUG, "ignore path: %s !", pFile);
					}
				}
			}
		}

		delete[] pFile;
		closedir(dp);
	}
}

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_zhonghong_scanner_core_MediaScanner
 * Method:    nativeScanDir
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_zhonghong_scanner_core_MediaScanner_nativeScanDir
  (JNIEnv *env, jobject obj, jstring dir) {
	const char *pDir = env->GetStringUTFChars(dir, NULL);
	if (pDir == NULL) {
		return;
	}

	MediaScannerCallback mscb(env, obj);
	doScanDir(pDir, mscb);

	env->ReleaseStringUTFChars(dir, pDir);
}

#ifdef __cplusplus
}
#endif
