package com.pqpo.android.asyncimageloader;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;

public class MemoryCache implements MemoryCacheInterface{

	private final Map<String, SoftReference<Bitmap>> softMap = Collections.synchronizedMap(new HashMap<String,SoftReference<Bitmap>>());
	
	@Override
	public boolean put(String key, Bitmap value) {
		softMap.put(key, new SoftReference<Bitmap>(value));
		return true;
	}

	@Override
	public Bitmap get(String key) {
		Bitmap bitmap = null;
		SoftReference<Bitmap> reference = softMap.get(key);
		if(reference!=null){
			bitmap = reference.get();
		}
		return bitmap;
	}

	@Override
	public Bitmap remove(String key) {
		SoftReference<Bitmap> remove = softMap.remove(key);
		return remove==null?null:remove.get();
	}

	@Override
	public Collection<String> keys() {
		return softMap.keySet();
	}

	@Override
	public void clear() {
		softMap.clear();
	}

}
