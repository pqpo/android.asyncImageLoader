package com.pqpo.android.asyncimageloader;

import java.util.Collection;

import android.graphics.Bitmap;

public interface MemoryCacheInterface {

	boolean put(String key,Bitmap value);
	Bitmap get(String key);
	Bitmap remove(String key);
	Collection<String> keys();
	void clear();
}
