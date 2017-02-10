package com.zhonghong.scanner.model;

public final class MediaInfo {
	private int mType;
	private int mVolumeId;
	private String mPath;
	
	public MediaInfo(int type, int volumeId, String path) {
		setInfo(type, volumeId, path);
	}
	
	public void setInfo(int type, int volumeId, String path) {
		mType = type;
		mVolumeId = volumeId;
		mPath = path;
	}
	
	public int getType() {
		return mType;
	}
	
	public int getVolumeId() {
		return mVolumeId;
	}
	
	public String getPath() {
		return mPath;
	}
	
	@Override
	public String toString() {
		return "mType: " + mType + ", mVolumeId: " + mVolumeId + ", mPath: " + mPath;
	}
}
