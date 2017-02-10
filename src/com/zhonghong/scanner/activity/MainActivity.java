package com.zhonghong.scanner.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.zhonghong.scanner.R;
import com.zhonghong.scanner.core.DatabaseHelper;
import com.zhonghong.scanner.core.MediaScanner;
import com.zhonghong.scanner.core.MediaScanner.IScanListener;
import com.zhonghong.scanner.model.MediaInfo;
import com.zhonghong.scanner.provider.MediaScannerProvider;
import com.zhonghong.scanner.service.MediaScannerService;
import com.zhonghong.scanner.utils.VolumeUtils;

public class MainActivity extends Activity {
	private static final String TAG = "MediaScanner";
	
	private DatabaseHelper mDBHelper;
	private ScanStateObserver mScanStateObserver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		String[] paths = VolumeUtils.getVolumePaths(this);
		for (String p : paths) {
			Log.i(TAG, "p: " + p);
		}
		
		mDBHelper = new DatabaseHelper(this);
		
		mScanStateObserver = new ScanStateObserver(this, null);
		mScanStateObserver.startObserve();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mScanStateObserver.stopObserve();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.test_scanner:
			MediaScanner ms = new MediaScanner(this);
			ms.registerScanListener(mScanListener);
			ms.scanDirs(new String[] { "/mnt/sdcard", "/mnt/SD1", "/storage/AC0F-07DF" });
			break;
		case R.id.test_scan_mode:
			Intent i = new Intent("zhonghong.intent.action.MEDIA_SCANNER_SCAN_ALL");
			sendBroadcast(i);
			break;
		case R.id.test_db:
			/*SQLiteDatabase db = mDBHelper.getReadableDatabase();
			Cursor c = db.query(DatabaseHelper.ScanStateTable.TABLE_NAME, null, null, null, null, null, null);
			if (c != null) {
				if (c.moveToNext()) {
					int state = c.getInt(c.getColumnIndex("state"));
					Log.d(TAG, "state: " + state);
				}
				
				c.close();
			}*/
			
			/*testScanState(false);*/
			
			int delRows = mDBHelper.deleteByVolumeIdAndPath(VolumeUtils.getVolumeIdByPath(this, "/storage/C26D-8D9B"), 
										"/storage/C26D-8D9B/Music/周杰伦");
			Log.i(TAG, "delRows: " + delRows);
			break;
		}
	}
	
	private void testScanState(boolean isBegin) {
		ContentValues vals = new ContentValues();
        vals.put(DatabaseHelper.ScanStateTable.STATE, isBegin ? 1 : 0);
        getContentResolver().update(MediaScannerProvider.SCAN_STATE_URI, vals, null, null);
	}
	
	private IScanListener mScanListener = new IScanListener() {
		
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
			Log.i(TAG, "onScanMediaInfo " + mi);
		}
	};
	
	/**
	 * 监听扫描状态
	 */
	private final class ScanStateObserver extends ContentObserver {
		private final Uri SCAN_STATE_URI = Uri.parse("content://com.zhonghong.scanner.provider/scanstate");
		private Context mContext;
		
		public ScanStateObserver(Context context, Handler handler) {
			super(handler);			
			mContext = context;
		}
		
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			Log.i(TAG, "onChange");
			if (SCAN_STATE_URI.equals(uri)) {
				Log.i(TAG, "state: " + getState());
			}
		}
		
		public int getState() {
			int state = 0;
			Cursor cursor = mContext.getContentResolver().query(SCAN_STATE_URI, null, null, null, null);
			
			if (cursor != null) {
				if (cursor.moveToNext()) {
					state = cursor.getInt(cursor.getColumnIndex("state"));
				}
				cursor.close();
			}
			
			return state;
		}
		
		public void startObserve() {
			mContext.getContentResolver().registerContentObserver(SCAN_STATE_URI, false, this);
		}
		
		public void stopObserve() {
			mContext.getContentResolver().unregisterContentObserver(this);
		}
	}
}
