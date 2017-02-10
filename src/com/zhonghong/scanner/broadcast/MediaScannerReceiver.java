package com.zhonghong.scanner.broadcast;

import com.zhonghong.scanner.service.MediaScannerService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class MediaScannerReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaScannerReceiver";
    private static final String ACTION_SCAN_ALL = "zhonghong.intent.action.MEDIA_SCANNER_SCAN_ALL";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final Uri uri = intent.getData();
        
        Log.i(TAG, "action: " + action);
        
        Intent scanIntent = new Intent(context, MediaScannerService.class);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || ACTION_SCAN_ALL.equals(action)) {
        	scanIntent.putExtra("scanmode", "all");
        	context.startService(scanIntent);
        } else {
            if ("file".equals(uri.getScheme())) {
            	if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
            		scanIntent.putExtra("scanmode", "mount");
            	} else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
            		scanIntent.putExtra("scanmode", "unmount");
            	} else {
            		scanIntent.putExtra("scanmode", "file");
            	}
            	
                final String path = uri.getPath();
                Log.i(TAG, "path: " + path);
                
                scanIntent.putExtra("path", path);
                context.startService(scanIntent);
            }
        }
    }
}
