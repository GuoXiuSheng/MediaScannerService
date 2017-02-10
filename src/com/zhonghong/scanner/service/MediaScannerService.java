package com.zhonghong.scanner.service;

import java.util.ArrayList;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.zhonghong.scanner.core.DatabaseHelper;
import com.zhonghong.scanner.core.MediaScanner;
import com.zhonghong.scanner.core.MediaScanner.IScanListener;
import com.zhonghong.scanner.model.MediaInfo;
import com.zhonghong.scanner.provider.MediaScannerProvider;
import com.zhonghong.scanner.utils.VolumeUtils;

public class MediaScannerService extends Service implements Runnable, IScanListener {
	private static final String TAG = "MediaScannerService";

	private static final int MSG_SCAN_MODE_ALL = 1;
	private static final int MSG_SCAN_MODE_MOUNT = 2;
	private static final int MSG_SCAN_MODE_UNMOUNT = 3;
	private static final int MSG_SCAN_MODE_FILE = 4;
	
	private DatabaseHelper mDBHelper;
	private MediaScanner mMediaScanner;
	private ArrayList<MediaInfo> mMediaInfos = new ArrayList<MediaInfo>();
	
	private Looper mScannerLooper;
	private ScannerHandler mScannerHandler;
	
	@Override
	public void onCreate() {
		Log.i(TAG, "onCreate");
		super.onCreate();
		
		mDBHelper = new DatabaseHelper(this);
		
		mMediaScanner = new MediaScanner(this);
		mMediaScanner.registerScanListener(this);
		
		Thread thr = new Thread(null, this, "MediaScannerThread");
        thr.start();
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
		
		// Make sure thread has started before telling it to quit.
		while (mScannerLooper == null) {
			synchronized (this) {
				try {
					wait(100);
				} catch (InterruptedException e) {
				}
			}
		}
        
		mScannerLooper.quit();		
		mMediaScanner.unregisterScanListener(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		while (mScannerHandler == null) {
            synchronized (this) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                }
            }
        }
		
		handleIntent(intent);		
		return Service.START_STICKY;
	}

	private void handleIntent(Intent intent) {
		if (intent == null) {
			return;
		}
		
		Message msg = mScannerHandler.obtainMessage();
        
        String scanModeStr = intent.getStringExtra("scanmode");
        if ("all".equals(scanModeStr)) {	
        	msg.what = MSG_SCAN_MODE_ALL;
        } else if ("mount".equals(scanModeStr)) {
        	msg.what = MSG_SCAN_MODE_MOUNT;
        	msg.obj = intent.getStringExtra("path");
        } else if ("unmount".equals(scanModeStr)) {
        	msg.what = MSG_SCAN_MODE_UNMOUNT;
        	msg.obj = intent.getStringExtra("path");
        } else if ("file".equals(scanModeStr)) {
        	msg.what = MSG_SCAN_MODE_FILE;
        	msg.obj = intent.getStringExtra("path");
        }
        
        mScannerHandler.sendMessageSync(msg);
	}

	@Override
	public void run() {
		// reduce priority below other background threads to avoid interfering
        // with other services at boot time.
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND +
                Process.THREAD_PRIORITY_LESS_FAVORABLE);
        Looper.prepare();

        mScannerLooper = Looper.myLooper();
        mScannerHandler = new ScannerHandler();

        Looper.loop();
	}

	private final class ScannerHandler extends Handler {
		private int mCountOfMessages = 0;
		
		void sendMessageSync(Message msg) {
			incMessage();			
			sendMessage(msg);
		}
		
		private synchronized void incMessage() {
			++mCountOfMessages;
		}
		
		private synchronized void decMessage() {
			--mCountOfMessages;
		}
		
		synchronized boolean hasMessages() {
			return mCountOfMessages != 0;
		}
		
		@Override
		public void handleMessage(Message msg) {
			decMessage();
			doScanBegin();
			
			// 移除设备直接删除数据库数据
			if (msg.what == MSG_SCAN_MODE_UNMOUNT) {
				deleteFromDBByVolumeId(new String[] { (String) msg.obj });
				doScanEnd();
				
				return;
			}
			
			String[] dirs = null;
			
			switch (msg.what) {
			case MSG_SCAN_MODE_ALL:
				dirs = VolumeUtils.getVolumePaths(MediaScannerService.this);
				
				// 扫描前先删除数据库相应的数据，保证扫描完后插入数据不重复
				deleteFromDBByVolumeId(dirs);
				break;
			case MSG_SCAN_MODE_MOUNT:
				dirs = new String[] { (String) msg.obj };
				deleteFromDBByVolumeId(dirs);
				break;
			case MSG_SCAN_MODE_FILE:
				dirs = new String[] { (String) msg.obj };
				deleteFromDBByVolumeIdAndPath((String) msg.obj);
				break;
			}
			
			if (dirs != null) {
				mMediaScanner.scanDirs(dirs);
			}
			
			doScanEnd();
		}
	}

	@Override
	public void onScanBegin() {
		Log.i(TAG, "onScanBegin");
	}

	@Override
	public void onScanEnd() {
		Log.i(TAG, "onScanEnd");
	}

	@Override
	public void onScanMediaInfo(MediaInfo mi) {
		mMediaInfos.add(mi);
	}
	
	private void doScanBegin() {
		Log.i(TAG, "doScanBegin");
		if (!mDBHelper.queryScanState()) {
			notifyScanState(true);
		}
	}
	
	private void doScanEnd() {
		Log.i(TAG, "doScanEnd");
		if (!mMediaInfos.isEmpty()) {
			mDBHelper.insertMediaInfos(mMediaInfos);
			mMediaInfos.clear();
		}
		
		// 如果之前是一系列的请求，线程里面还有消息没处理，这里则不更新扫描状态
		if (!mScannerHandler.hasMessages()) {
			notifyScanState(false);
		}
	}
	
	private void notifyScanState(boolean isBegin) {
		Log.i(TAG, "notifyScanState isBegin: " + isBegin);
		ContentValues vals = new ContentValues();
        vals.put(DatabaseHelper.ScanStateTable.STATE, isBegin ? 1 : 0);
        getContentResolver().update(MediaScannerProvider.SCAN_STATE_URI, vals, null, null);
	}
	
	/**
	 * 删除指定存储设备的所有数据
	 */
	private void deleteFromDBByVolumeId(String[] volumePaths) {
		if (volumePaths == null) {
			return;
		}
		
		int delRows;
		for (String vp : volumePaths) {
			delRows = mDBHelper.deleteByVolumeId(VolumeUtils.getVolumeIdByPath(this, vp));
			Log.i(TAG, "deleteFromDB vp: " + vp + ", delRows: " + delRows);
		}
	}
	
	private void deleteFromDBByVolumeIdAndPath(String path) {
		if ((path == null) || path.isEmpty()) {
			return;
		}
		
		if (path.charAt(path.length() - 1) != '/') {
			path += '/';
		}
		
		int delRows = mDBHelper.deleteByVolumeIdAndPath(VolumeUtils.getVolumeIdByPath(this, path), path);
		Log.i(TAG, "deleteByVolumeIdAndPath path: " + path + ", delRows: " + delRows);
	}
}
