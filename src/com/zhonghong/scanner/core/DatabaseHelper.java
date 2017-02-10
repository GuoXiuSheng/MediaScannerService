package com.zhonghong.scanner.core;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.zhonghong.scanner.model.MediaInfo;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "DatabaseHelper";
	
	public static final int MEDIA_TYPE_ERR = -1;
	public static final int MEDIA_TYPE_AUDIO = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	public static final int MEDIA_TYPE_IMAGE = 3;
	
	private static final String MEDIA_DB_NAME = "media.db";
	
	private static final String CREATE_MEDIA_INFO_TABLE = "create table " + MediaInfoTable.TABLE_NAME + " ("
			+ "id integer primary key autoincrement, "
			+ MediaInfoTable.TYPE + " integer, "
			+ MediaInfoTable.VOLUME_ID + " integer, "
			+ MediaInfoTable.PATH + " text)";	
	private static final String CREATE_SCAN_STATE_TABLE = "create table " + ScanStateTable.TABLE_NAME + " ("
			+ "id integer primary key autoincrement, "
			+ ScanStateTable.STATE + " integer)";
	
	private static final String DROP_MEDIA_INFO_TABLE = "drop table if exists " + MediaInfoTable.TABLE_NAME;
	private static final String DROP_SCAN_STATE_TABLE = "drop table if exists " + ScanStateTable.TABLE_NAME;
	
	private static final String INIT_SCAN_STATE_TABLE = "insert into " + ScanStateTable.TABLE_NAME 
			+ "(" + ScanStateTable.STATE + ") values(0)";
	
	private static final String INSERT_MEDIA_INFO = "insert into " + MediaInfoTable.TABLE_NAME + "("
			+ MediaInfoTable.TYPE + ", " + MediaInfoTable.VOLUME_ID + ", " + MediaInfoTable.PATH + ") "
			+ "values(?,?,?)";
	
	public DatabaseHelper(Context context) {
		super(context, MEDIA_DB_NAME, null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(TAG, "onCreate");
		
		db.execSQL(CREATE_MEDIA_INFO_TABLE);
		db.execSQL(CREATE_SCAN_STATE_TABLE);
		
		// 创建表后, 初始化扫描状态, 后续直接update; 说明: 1 表示扫描开始, 0 表示扫描结束
		db.execSQL(INIT_SCAN_STATE_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "onUpgrade");
		
		db.execSQL(DROP_MEDIA_INFO_TABLE);
		db.execSQL(DROP_SCAN_STATE_TABLE);
		
		onCreate(db);
	}
	
	/**
	 * 批量插入数据
	 */
	public boolean insertMediaInfos(ArrayList<MediaInfo> mediaInfos) {
		Log.i(TAG, "insertMediaInfos");
		SQLiteDatabase db = getWritableDatabase();
		SQLiteStatement stat = db.compileStatement(INSERT_MEDIA_INFO);  
		
		// 开启事务
		db.beginTransaction();
		try {
			for (MediaInfo mi : mediaInfos) {
				stat.bindLong(1, mi.getType());  
                stat.bindLong(2, mi.getVolumeId());  
                stat.bindString(3, mi.getPath());
                stat.executeInsert();  
			}

			// 设置事务标志为成功，当结束事务时就会提交事务
			db.setTransactionSuccessful();
			Log.i(TAG, "insertMediaInfos success!");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 结束事务
			db.endTransaction();
		}
		
		return true;
	}
	
	public int deleteByVolumeId(int volumeId) {
		SQLiteDatabase db = getWritableDatabase();
		return db.delete(MediaInfoTable.TABLE_NAME, MediaInfoTable.VOLUME_ID + " = ?", new String[] { "" +  volumeId});
	}
	
	public int deleteByVolumeIdAndPath(int volumeId, String path) {
		SQLiteDatabase db = getWritableDatabase();
		return db.delete(MediaInfoTable.TABLE_NAME, MediaInfoTable.VOLUME_ID + " = ? AND " + MediaInfoTable.PATH + " LIKE ? "
				, new String[] { "" +  volumeId, path + "%" });
	}
	
	public boolean queryScanState() {
		boolean isScanning = false;
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(ScanStateTable.TABLE_NAME, null, null, null, null, null, null);
		
		if (c != null) {
			if (c.moveToNext()) {
				final int index = c.getColumnIndex(ScanStateTable.STATE);
				isScanning = (index != -1) ? (c.getInt(index) == 1) : false;
			}
			
			c.close();
		}
		
		return isScanning;
	}
	
	public static final class MediaInfoTable {
		public static final String TABLE_NAME = "mediainfo";
		public static final String TYPE = "type";
		public static final String VOLUME_ID = "volume_id";
		public static final String PATH = "path";
	}
	
	public static final class ScanStateTable {
		public static final String TABLE_NAME = "scanstate";
		public static final String STATE = "state";
	}
}
