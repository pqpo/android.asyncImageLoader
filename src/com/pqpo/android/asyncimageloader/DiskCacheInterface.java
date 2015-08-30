package com.pqpo.android.asyncimageloader;

import java.io.File;
import java.io.IOException;

import android.graphics.Bitmap;

public interface DiskCacheInterface {

	File getDirectory();
	Bitmap get(String key);
	boolean save(String key,Bitmap bitmap)throws IOException;
	boolean remove(String key);
	void clear();
	void close();
}
