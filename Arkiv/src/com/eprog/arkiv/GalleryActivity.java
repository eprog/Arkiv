package com.eprog.arkiv;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;

public class GalleryActivity extends Activity {
	
	private String[] mImages;
	private String startFolder;
	private String folder;
	
	private ImageAdapter adapter;
	
	private Gallery gallery;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.gallery);
	    
	    // Avoid screen rotation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    
	    Bundle b = getIntent().getExtras();
	    folder = b.getString("folder");
	    startFolder = folder;
	    
	    // List folder and add paths to mImages
	    File dir = new File(folder);
	    File[] files = dir.listFiles();
	    mImages = new String[files.length];
	    for (int i = 0; i < files.length; i++) {
	    	mImages[i] = files[i].getPath();
	    }
	    
	    gallery = (Gallery) findViewById(R.id.gallery);
	    adapter = new ImageAdapter(this);
	    gallery.setAdapter(adapter);

	    gallery.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView parent, View v, int position, long id) {
	            Toast.makeText(GalleryActivity.this, mImages[position], Toast.LENGTH_LONG).show();
	            
	            // Check if the selected item is a file or a folder
	            File selected = new File(mImages[position]);
	            if (selected.isDirectory()) {
	            	// Open directory
	            	folder = mImages[position];
	            	
	            	File[] files = selected.listFiles();
	        	    mImages = new String[files.length];
	        	    for (int i = 0; i < files.length; i++) {
	        	    	mImages[i] = files[i].getPath();
	        	    }
	            	adapter.notifyDataSetChanged();
	            	gallery.setSelection(0);
	            	
	            	ImageView imageView = (ImageView) findViewById(R.id.ImageView01);
	            	imageView.setImageResource(0); //setImageBitmap(loadBitmap(mImages[position], 2));
	            } else {
	            	// File selected
	            	ImageView imageView = (ImageView) findViewById(R.id.ImageView01);
	            	imageView.setImageBitmap(loadBitmap(mImages[position], 2));
	            }
	        }
	    });
	}
	
	
	
	@Override
	public void onBackPressed() {
		// Check if activity should be finished or move up
		if (folder.equals(startFolder)) {
			finish();
		} else {
			File dir = new File(folder);
			folder = dir.getParent();
			
			dir = new File(folder);
			File[] files = dir.listFiles();
    	    mImages = new String[files.length];
    	    for (int i = 0; i < files.length; i++) {
    	    	mImages[i] = files[i].getPath();
    	    }
    	    adapter.notifyDataSetChanged();
    	    gallery.setSelection(0);
    	    
    	    ImageView imageView = (ImageView) findViewById(R.id.ImageView01);
        	imageView.setImageDrawable(null);
		}
	}

	/**
	 * 
	 * @param path
	 * @param size
	 * @return
	 */
	private Bitmap loadBitmap(String path, int size) {
    	
		Bitmap bitmap = null;
		try {
    	BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inSampleSize = size;
    	bitmap = BitmapFactory.decodeFile(path, options);
		} catch (OutOfMemoryError e) {
			Log.d("Arkiv", "GalleryActivity out of memory: " + e);
		}
    	return bitmap;

    }
	
	public class ImageAdapter extends BaseAdapter {
	    int mGalleryItemBackground;
	    private Context mContext;

	    public ImageAdapter(Context c) {
	        mContext = c;
	        TypedArray attr = mContext.obtainStyledAttributes(R.styleable.Gallery);
	        mGalleryItemBackground = attr.getResourceId(
	                R.styleable.Gallery_android_galleryItemBackground, 0);
	        attr.recycle();
	    }

	    public int getCount() {
	        return mImages.length;
	    }

	    public Object getItem(int position) {
	        return position;
	    }

	    public long getItemId(int position) {
	        return position;
	    }

	    public View getView(int position, View convertView, ViewGroup parent) {
	        ImageView imageView = new ImageView(mContext);
	        
	     // Check if the selected item is a file or a folder
            File selected = new File(mImages[position]);
            if (selected.isDirectory()) {
            	File[] children = selected.listFiles();
            	if (children.length > 0) {
            		Bitmap bitmap = loadBitmap(children[0].getPath(), 8);     
            		Bitmap bitmap2 = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
            		// Show folder name (title/textview etc)
            		Canvas canvas = new Canvas(bitmap2);
            		String folderName = mImages[position];
            		folderName = folderName.substring(folderName.lastIndexOf('/'));
            		//Log.d("Arkiv", "pos: " + position + " folder; " + folderName);
            		Paint paint = new Paint();
            		paint.setColor(Color.RED);
            		Log.d("Arkiv", "textSize: " + bitmap.getWidth()/8);
            		paint.setTextSize(bitmap.getWidth()/8);
            		canvas.drawBitmap(bitmap, 0, 0, paint);
            		canvas.drawText (folderName.substring(folderName.lastIndexOf('/')), 10, bitmap.getHeight()/2, paint);            		
            		imageView.setImageBitmap(bitmap2);
            		imageView.setBackgroundColor(Color.RED);
            	}
            } else {

            	//imageView.setImageResource(mImageIds[position]);
            	imageView.setImageBitmap(loadBitmap(mImages[position], 8));
            }
            imageView.setLayoutParams(new Gallery.LayoutParams(150, 100));
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            imageView.setBackgroundResource(mGalleryItemBackground);

	        return imageView;
	    }
	    
	}

}
