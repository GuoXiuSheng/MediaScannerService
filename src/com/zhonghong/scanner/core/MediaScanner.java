package com.zhonghong.scanner.core;

import java.util.ArrayList;
import java.util.List;

import com.zhonghong.scanner.model.MediaInfo;
import com.zhonghong.scanner.utils.VolumeUtils;

import android.content.Context;

public class MediaScanner {
	private static final String TAG = "MediaScanner";
	
	private Context mContext;

	private List<IScanListener> mScanListeners = new ArrayList<IScanListener>();
	private int mCurVolumeId;
	
	public MediaScanner(Context context) {
		mContext = context;
	}

	/**
	 * 递归扫描目录
	 */
	public void scanDirs(String[] dirs) {
		if (dirs == null) {
			return;
		}

		notifyScanBegin();
		
		for (String dir : dirs) {
			mCurVolumeId = VolumeUtils.getVolumeIdByPath(mContext, dir);
			nativeScanDir(dir);
		}
		
		notifyScanEnd();
	}

	/**
	 * jni层递归扫描到的文件回调接口
	 */
	private void handleScanFileFromNative(int type, String filePath) {
		notifyScanMediaInfo(new MediaInfo(type, mCurVolumeId, filePath));
	}

	public void registerScanListener(IScanListener listener) {
		if ((listener != null) && !mScanListeners.contains(listener)) {
			mScanListeners.add(listener);
		}
	}

	public void unregisterScanListener(IScanListener listener) {
		if (mScanListeners.contains(listener)) {
			mScanListeners.remove(listener);
		}
	}
	
	private void notifyScanBegin() {
		for (IScanListener sl : mScanListeners) {
			sl.onScanBegin();
		}
	}
	
	private void notifyScanEnd() {
		for (IScanListener sl : mScanListeners) {
			sl.onScanEnd();
		}
	}

	private void notifyScanMediaInfo(MediaInfo mi) {
		for (IScanListener sl : mScanListeners) {
			sl.onScanMediaInfo(mi);
		}
	}

	public interface IScanListener {
		void onScanBegin();
		void onScanEnd();
		void onScanMediaInfo(MediaInfo mi);
	}

	private native void nativeScanDir(String dir);
	
	static { 
		System.loadLibrary("MediaScanner");
	}
}
