package com.zhonghong.scanner.utils;

import java.lang.reflect.Method;

import android.content.Context;
import android.os.storage.StorageManager;

public final class VolumeUtils {
	private static String[] sVolumePaths;
	
	public static String[] getVolumePaths(Context context) {
		if (sVolumePaths == null) {
			StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
			
			try {
				Class<?> cls = sm.getClass();
				Method getVolumePathsMethod = cls.getMethod("getVolumePaths", null);
				sVolumePaths = (String[]) getVolumePathsMethod.invoke(sm, null);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		
		return sVolumePaths;
	}
	
	public static int getVolumeIdByPath(Context context, String path) {
		if (sVolumePaths == null) {
			sVolumePaths = getVolumePaths(context);
		}
		
		if (sVolumePaths != null) {
			int index = 0;
			for (String p : sVolumePaths) {
				if (path.equals(p) || path.startsWith(p + "/")) {
					return index;
				}
					
				index++;
			}
		}
		
		return -1;
	}
}
