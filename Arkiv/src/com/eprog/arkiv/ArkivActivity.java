package com.eprog.arkiv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class ArkivActivity extends BaseActivity implements SurfaceHolder.Callback {
	private SurfaceView surface = null;
	private SurfaceHolder holder = null;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // Avoid screen rotation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	       
        for (int i = 1; i < 7; i++) {
        	createFolder(Settings.PREF_CATEGORY + i);
        }        
        
        // Register context menu for buttons
        Button b = null;
		String label = null;
		String buttonID = null;
		int resID = 0;
		
		for (int i = 1; i < 7; i++) {
			buttonID = "buttonCategory" + i;
			resID = getResources().getIdentifier(buttonID, "id", "com.eprog.arkiv");
			b = (Button)findViewById(resID);
			registerForContextMenu(b);
		}
        
    }
    
    @Override
	protected void onPause() {
    	Log.d("Arkiv", "onPause()");
		super.onPause();
		deactivateCamera();
		holder.removeCallback(this);
		surface = null;
    }

	@Override
	protected void onResume() {
		Log.d("Arkiv", "onResume()");
		super.onResume();
		
		Boolean statusbar = settings.getBoolean("PREF_STATUS_BAR", true);
		if (statusbar) {
			WindowManager.LayoutParams attrs = getWindow().getAttributes();
	        attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
	        getWindow().setAttributes(attrs);
		} else {
			WindowManager.LayoutParams attrs = getWindow().getAttributes();
	        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
	        getWindow().setAttributes(attrs);
		}
		
		surface = (SurfaceView)findViewById(R.id.surface);
		
		// Change button text if necessary
		setButtonLabels();
		
		holder = surface.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.setFixedSize(300, 200);
		
//		Boolean onTop = settings.getBoolean("PREF_BUTTONS_ON_TOP", true);
//		if (onTop) {
//			setContentView(R.layout.main);
//		} else {
//			setContentView(R.layout.main2);
//		}
        
        // Show help dialog on first start
        if (settings.getBoolean(Settings.PREF_FIRST_START, true)) {
        	Editor editor = settings.edit();
        	editor.putBoolean(Settings.PREF_FIRST_START, false);
        	editor.commit();
        	showHelpDialog();
        }
	}

    
    public void clickHandler(View view) {
		switch (view.getId()) {
		case R.id.buttonCategory1:
			takePicture(getResources().getString(R.string.folderIntyg), getResources().getString(view.getId()));
			break;
		case R.id.buttonCategory2:
			takePicture(getResources().getString(R.string.folderLedighet), getResources().getString(view.getId()));
			break;

		case R.id.buttonCategory3:
			takePicture(getResources().getString(R.string.folderKvitto), getResources().getString(view.getId()));
			break;

		case R.id.buttonCategory4:
			takePicture(getResources().getString(R.string.folderFaktura), getResources().getString(view.getId()));
			break;

		case R.id.buttonCategory5:
			takePicture(getResources().getString(R.string.folderAd), getResources().getString(view.getId()));
			break;
			
		case R.id.buttonCategory6:
			takePicture(getResources().getString(R.string.folderOther), getResources().getString(view.getId()));
			break;

		case R.id.buttonLog:
	        Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.chooserTitle)), SELECT_FROM_ARCHIVE);
			break;
		}
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			// Operation was cancelled, reactivate camera if needed
			activateCamera(holder);
			return;
		}
		 
        Bitmap bitmap   = null;
        String path     = "";
 
        if (requestCode == SELECT_FROM_ARCHIVE) {
            selectedImageUri = data.getData();
            path = getRealPathFromURI(selectedImageUri); //from Gallery
 
            if (path == null)
                path = selectedImageUri.getPath(); //from File Manager
 
            if (path != null)
                bitmap  = BitmapFactory.decodeFile(path);
        }
        imageView.setImageBitmap(bitmap);
	}
	
	public String getRealPathFromURI(Uri contentUri) {
        String [] proj      = {MediaStore.Images.Media.DATA};
        Cursor cursor       = managedQuery( contentUri, proj, null, null,null);
 
        if (cursor == null) {
        	return null;
        }
 
        int column_index    = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
	
	private void activateCamera(SurfaceHolder holder) {
		Log.d("Arkiv", "activateCamera() camera = " + camera);
		if (camera != null) {
			deactivateCamera();
		}
		try {
			camera = Camera.open();
			camera.setPreviewDisplay(holder);
			Display display = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			int rotation = display.getRotation();
			if (rotation == 0) {
				camera.setDisplayOrientation(90);
			} else {
				camera.setDisplayOrientation(0);
			}
			Parameters params = camera.getParameters();
			params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			params.setJpegQuality(60);
			camera.setParameters(params);
			camera.startPreview();						
		} catch (IOException e) {
			Log.d("Arkiv", e.getMessage());
			deactivateCamera();
		}
	}
	
	private void deactivateCamera() {
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}

	private void takePicture(String folder, String category) {		
		Log.d("Arkiv", "takePicture()");
		this.folder = folder;
		this.category = category;
	
		if (camera != null) {
//			int sdk = android.os.Build.VERSION.SDK_INT;
//			if (sdk > 10) {
//				camera.takePicture(shutterCallback, rawCallback, jpegCallback);  // TODO Remove this special case when AutoFocus works on X10 mini with ICS. 
//			} else {
				camera.autoFocus(new AutoFocusCallback() {

					public void onAutoFocus(boolean success, Camera camera) {
						Log.d("Arkiv", "onAutoFocus()");
						if (success) {
							camera.takePicture(shutterCallback, rawCallback, jpegCallback);
						} else {
							// Inform user that the camera could not focus.
							Toast toast = Toast.makeText(getApplicationContext(), R.string.noFocus, Toast.LENGTH_SHORT);
							toast.show();
						}
					}
				});
//			}
		}
	}
	
	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
		}
	};
	
	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			
		}
	};
	
	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			
			Calendar c = Calendar.getInstance();
	        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
	        String formattedDate = df.format(c.getTime());
	        
	        filename = category + formattedDate + ".jpg";
			
	        // Save the image to the right folder on the SD card
			FileOutputStream outStream = null;
			try {
				outStream = new FileOutputStream(folder + "/" + filename);
				outStream.write(data);
				outStream.close();
			} catch (FileNotFoundException e) {
				Log.d("onPictureTaken", e.getMessage());
				Toast toast = Toast.makeText(getApplicationContext(), R.string.unableToSaveImage, Toast.LENGTH_LONG);
				toast.show();
				return;
			} catch (IOException e) {
				Log.d("onPictureTaken", e.getMessage());
				Toast toast = Toast.makeText(getApplicationContext(), R.string.unableToSaveImage, Toast.LENGTH_LONG);
				toast.show();
				return;
			}
			
			// Insert image into Media Store
	    	ContentValues content = new ContentValues(1);
	    	content.put(Images.Media.MIME_TYPE, "image/jpg");
	    	content.put(MediaStore.Images.Media.DATA, folder + "/" + filename);
	    	
	    	ContentResolver resolver = getContentResolver();
	    	Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, content);
	    	sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
			
	    	// Check if subcategory dialog shall be shown
	    	Boolean subCategories = settings.getBoolean("PREF_SUB_CATEGORIES", false);
	    	if (subCategories) {
	    		showSubCategoryDialog();
	    	} else {
	    		sendMail(category, folder, filename);
	    		camera.startPreview();  // TODO Is this needed?
	    	}
		}
	};
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d("Arkiv", "surfaceChanged()");
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("Arkiv", "surfaceCreated()");
		activateCamera(holder);
	}
	
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("Arkiv", "surfaceDestroyed()");
		deactivateCamera();
	}
	
}