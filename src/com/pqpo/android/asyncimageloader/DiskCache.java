package com.pqpo.android.asyncimageloader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

public class DiskCache implements DiskCacheInterface{

	private final File root;

	public DiskCache(File root) throws IOException{
		this.root = root;
		if(!root.exists())root.mkdir();
	}

	@Override
	public File getDirectory() {
		return root;
	}

	@Override
	public Bitmap get(String key) {
		Bitmap bitmap = null;
		File newFile = new File(root, key);
		if(newFile.exists()){
			bitmap = BitmapFactory.decodeFile(newFile.getAbsolutePath());
		}
		return bitmap;
	}

	@Override
	public boolean save(String key, Bitmap bitmap) throws IOException {
		File file = new File(root, key);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
		boolean b = bitmap.compress(CompressFormat.PNG, 80, bos);
		bos.flush();
		bos.close();
		bitmap = null;
		return b;
	}

	@Override
	public boolean remove(String key) {
		File file = new File(root, key);
		boolean b = false;
		if(file.exists()){
			b = file.delete();
		}
		return b;
	}

	@Override
	public void clear() {
		File[] files = root.listFiles();
		for(File f:files){
			if(f!=null&&f.exists()){
				f.delete();
			}
		}
	}

	@Override
	public void close() {
		//do nothing
	}

}
