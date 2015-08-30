package com.pqpo.android.asyncimageloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

public class AsynImageLoader{

	private static final String TAG = AsynImageLoader.class.getSimpleName();
	private Options options;
	private DiskCache diskCache;
	private MemoryCache memoryCache;
	private ExecutorService threadPool;
	private LoaderHandler handler=new LoaderHandler();
	private boolean isDebug = false;

	private AsynImageLoader(Options options){
		this.options = options;
		isDebug = options.debug;
		if(options.memoryCacheable){
			memoryCache = new MemoryCache();
		}
		if(options.diskCacheable){
			try {
				diskCache = new DiskCache(options.diskCacheDir);
			} catch (IOException e) {
				e.printStackTrace();
				Log.i(TAG,"Cannot create a disk cache folder,Disk cache invalidation!");
			}
		}
	}

	/**
	 * 异步加载并显示图片
	 * @param imageView 加载到imageview对象
	 * @param imgUrl 图片网络路径
	 * @param key 图片key,用于获取内存缓存已经磁盘缓存名
	 */
	public void displayImage(ImageView imageView,URL imgUrl,String key){
		Bitmap bitmap = null;
		boolean memory = (memoryCache!=null);
		boolean disk = (diskCache!=null);
		if(options.imageOnLoading!=-1)imageView.setImageResource(options.imageOnLoading);
		if(memory){
			if((bitmap = memoryCache.get(key))!=null){
				imageView.setImageBitmap(bitmap);
				if(isDebug)Log.d(TAG,"Load image from memory! key = "+key);
				return;
			}
		}
		if(disk){
			if((bitmap = diskCache.get(key))!=null){
				imageView.setImageBitmap(bitmap);
				if(isDebug)Log.d(TAG,"Load image from disk! key = " +key);
				if(memory){
					memoryCache.put(key, bitmap);
				}
				return;
			}
		}
		if(threadPool==null){
			initThreadPool();
		}
		handler.setImageView(imageView);
		handler.setKey(key);
		threadPool.execute(new LoaderRunnable(imgUrl));
	}

	private class LoaderRunnable implements Runnable{

		private URL url;
		public LoaderRunnable(URL url) {
			this.url = url;
		}

		@Override
		public void run() {
			try {
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setReadTimeout(10000);
				conn.setConnectTimeout(10000);
				InputStream is = conn.getInputStream();
				Bitmap bitmap = BitmapFactory.decodeStream(is);
				if(bitmap!=null){
					float ch = bitmap.getHeight();
					float cw = bitmap.getWidth();
					if (ch>options.height||cw>options.width) {
						Matrix x = new Matrix();
						float h = options.height/ch;
						float w = options.width/cw;
						if(h>w){
							x.postScale(w, w);
						}else{
							x.postScale(h, h);
						}
						bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), x, true);
					}
					handler.obtainMessage(0, bitmap).sendToTarget();//获取图片成功
				}else{
					handler.obtainMessage(1).sendToTarget();//获取图片失败
				}
				is.close();
				conn.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private synchronized void initThreadPool(){
		threadPool = Executors.newFixedThreadPool(options.threadPoolSize);
	}

	@SuppressLint("HandlerLeak") 
	private class LoaderHandler extends Handler{
		private ImageView imageView;
		private String key = "default";

		public void setImageView(ImageView imageView) {
			this.imageView = imageView;
		}

		public void setKey(String key) {
			this.key = key;
		}

		@Override
		public void handleMessage(Message msg) {
			if(msg.what==0){
				Object obj = msg.obj;
				if(obj instanceof Bitmap){
					Bitmap b = (Bitmap) obj;
					if(imageView!=null) imageView.setImageBitmap(b);
					if(isDebug)Log.d(TAG,"Load image from net! key = " +key+"width="+b.getWidth()+"height="+b.getHeight());
					if(memoryCache!=null)memoryCache.put(key, (Bitmap)obj);
					try {
						if(diskCache!=null)diskCache.save(key, (Bitmap)obj);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				obj = null;
			}else if(msg.what==1){
				if(options.imageOnFail!=-1&&imageView!=null)imageView.setImageResource(options.imageOnFail);
			}
		}
	}

	/**
	 * 清理内存缓存
	 */
	public void clearMemoryCache(){
		if(memoryCache==null) return;
		memoryCache.clear();
	}
	/**
	 * 清理磁盘缓存
	 */
	public void clearDiskCache(){
		if(diskCache==null)return;
		diskCache.clear();
	}
	/**
	 * 释放资源
	 * @param clearDisk
	 */
	public void releas(boolean clearDisk){
		if(isDebug)Log.d(TAG,"Releas!");
		if(clearDisk){
			clearDiskCache();
		}
		clearMemoryCache();
		if(threadPool!=null)threadPool.shutdown();
	}

	public static class Options{
		public Context context;
		public int threadPoolSize = 3;
		public boolean diskCacheable = false;
		public boolean memoryCacheable = false;
		public int imageOnLoading = -1;
		public int imageOnFail = -1;
		public boolean debug = false;
		public float height = 800.0f;
		public float width = 320.0f;

		public File diskCacheDir = new File(Environment.getExternalStorageDirectory()
				+File.separator+"asynImageLoader"+File.separator);
	}

	public static class Builder{

		private Options options = new Options();

		/**
		 * 是否启用内存缓存
		 * @param b
		 * @return
		 */
		public  Builder cacheInMemory(boolean b){
			options.memoryCacheable = b;
			return this;
		}
		/**
		 * 是否启用磁盘缓存
		 * @param b
		 * @return
		 */
		public Builder cacheOnDisk(boolean b){
			options.diskCacheable = b;
			return this;
		}
		/**
		 * 显示真正加载图片
		 * @param res
		 * @return
		 */
		public Builder showImageOnLoading(int res){
			options.imageOnLoading = res;
			return this;
		}
		/**
		 * 显示加载失败图片
		 * @param res
		 * @return
		 */
		public Builder showImageOnFail(int res){
			options.imageOnFail = res;
			return this;
		}
		/**
		 * 是否显示调试Log
		 * @param b
		 * @return
		 */
		public Builder showDebugLogs(boolean b){
			options.debug = b;
			return this;
		}
		/**
		 * 线程池最大线程数
		 * @param i
		 * @return
		 */
		public Builder threadPoolSize(int i){
			options.threadPoolSize = i;
			return this;
		}
		/**
		 * 磁盘缓存路径
		 * @param dir
		 * @return
		 */
		public Builder diskCacheDir(File dir){
			options.diskCacheDir = dir;
			return this;
		}
		/**
		 * 图片缓存最大尺寸
		 * @param dir
		 * @return
		 */
		public Builder cacheExtraOptions(float width,float height){
			options.width = width;
			options.height = height;
			return this;
		}
		/**
		 * 创建AsynImageLoader
		 * @param context
		 * @return
		 */
		public synchronized AsynImageLoader build(Context context){
			options.context = context;
			AsynImageLoader loader = new AsynImageLoader(options);
			return loader;
		}
	}
}
