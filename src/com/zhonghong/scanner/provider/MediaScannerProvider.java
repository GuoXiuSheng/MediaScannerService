package com.zhonghong.scanner.provider;

import com.zhonghong.scanner.core.DatabaseHelper;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class MediaScannerProvider extends ContentProvider {
	private static final String AUTHORITY = "com.zhonghong.scanner.provider";
	
	public static final Uri MEDIA_URI = Uri.parse("content://" + AUTHORITY + "/media");	
	public static final Uri SCAN_STATE_URI = Uri.parse("content://" + AUTHORITY + "/scanstate");
	
	private static final int MEDIA_CODE = 1;
	private static final int SCAN_STATE_CODE = 2;
	
	private static UriMatcher sUriMatcher;
	
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, "media", MEDIA_CODE);
		sUriMatcher.addURI(AUTHORITY, "scanstate", SCAN_STATE_CODE);
	}
	
	private DatabaseHelper mDBHelper;

	@Override
	public boolean onCreate() {
		mDBHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = mDBHelper.getReadableDatabase();
		Cursor cursor = null;
		
		switch (sUriMatcher.match(uri)) {
		case MEDIA_CODE:
			cursor = db.query(DatabaseHelper.MediaInfoTable.TABLE_NAME, projection, 
						selection, selectionArgs, null, null, sortOrder);
			break;
		case SCAN_STATE_CODE:
			cursor = db.query(DatabaseHelper.ScanStateTable.TABLE_NAME, projection, 
						selection, selectionArgs, null, null, sortOrder);
			break;
		default:
			break;
		}
		
		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDBHelper.getReadableDatabase();
		int result = -1;
		
		switch (sUriMatcher.match(uri)) {
		case SCAN_STATE_CODE:
			result = db.update(DatabaseHelper.ScanStateTable.TABLE_NAME, values, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(SCAN_STATE_URI, null);
			break;
		default:
			break;
		}
				
		return result;
	}
	
	@Override
	public String getType(Uri uri) {
		return null;
	}
}
