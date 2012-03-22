package com.eprog.arkiv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

public class ArkivActivity extends Activity implements SurfaceHolder.Callback {
	private static final int SELECT_PROGRAM = 0;
	private static final int SELECT_FROM_ARCHIVE = 1;
	private Camera camera = null;
	private String folder = null;
	private Uri selectedImageUri;
	private ImageView imageView;
	private SharedPreferences settings;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

//        Boolean onTop = settings.getBoolean("PREF_BUTTONS_ON_TOP", true);
//		if (onTop) {
//			setContentView(R.layout.main);
//		} else {
//			setContentView(R.layout.main2);
//		}
		
        // Create folders
        createFolder("/sdcard/Arkiv/Intyg");
        createFolder("/sdcard/Arkiv/Ledighet");
        createFolder("/sdcard/Arkiv/Kvitto");
        createFolder("/sdcard/Arkiv/Faktura");
        createFolder("/sdcard/Arkiv/Other");
        
        SurfaceView surface = (SurfaceView)findViewById(R.id.surface);
        SurfaceHolder holder = surface.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.setFixedSize(300, 200);
    }
    
    @Override
	protected void onPause() {
		super.onPause();
		deactivateCamera();
	}

	@Override
	protected void onResume() {
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
		
//		Boolean onTop = settings.getBoolean("PREF_BUTTONS_ON_TOP", true);
//		if (onTop) {
//			setContentView(R.layout.main);
//		} else {
//			setContentView(R.layout.main2);
//		}
		
	}

	private void createFolder(String path) {
    	File folder = new File(path);
        boolean success = false;
        if(!folder.exists())
        {
            success = folder.mkdirs();
        }         
        if (!success) 
        { 
            Log.d("createFolder", "Folders could not be created");
        }
        
    }
    
    public void clickHandler(View view) {
		switch (view.getId()) {
		case R.id.buttonIntyg:
			takePicture("Intyg");
			break;
		case R.id.buttonLedighet:
			takePicture("Ledighet");
			break;

		case R.id.buttonKvitto:
			takePicture("Kvitto");
			break;

		case R.id.buttonFaktura:
			takePicture("Faktura");
			break;

		case R.id.buttonOther:
			takePicture("Other");
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
			activateCamera(null);
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
		if (camera != null) {
			deactivateCamera();
		}
		try {
			camera = Camera.open();
			if (holder == null) {
				SurfaceView surface = (SurfaceView)findViewById(R.id.surface);
				holder = surface.getHolder();
			}
			camera.setPreviewDisplay(holder);
			camera.setDisplayOrientation(90);
			camera.autoFocus(autoFocusCallback);
			Parameters params = camera.getParameters();
			params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
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

	private void takePicture(String folder) {		
		this.folder = folder;
	
		if (camera != null) {
			camera.takePicture(shutterCallback, rawCallback, jpegCallback);
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
	        
			String dirPath = "/sdcard/Arkiv/" + folder;
	        String filename = folder + formattedDate + ".jpg";
			
	        // Save the image to the right folder on the SD card
			FileOutputStream outStream = null;
			try {
				outStream = new FileOutputStream(dirPath + "/" + filename);
				outStream.write(data);
				outStream.close();
			} catch (FileNotFoundException e) {
				Log.d("onPictureTaken", e.getMessage());
			} catch (IOException e) {
				Log.d("onPictureTaken", e.getMessage());
			}
			
			sendMail(folder, dirPath, filename);
			camera.startPreview();
		}
	};
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	public void surfaceCreated(SurfaceHolder holder) {
		activateCamera(holder);
	}
	
	AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {

		public void onAutoFocus(boolean arg0, Camera arg1) {
			// TODO Auto-generated method stub
		}
	};

	public void surfaceDestroyed(SurfaceHolder holder) {
		deactivateCamera();
	}
	
	private void sendMail(String type, String folder, String filename) {
		// Check if mail shall be sent
		Boolean sendMail = settings.getBoolean("PREF_SEND_MAIL", false);
		String emailAddress = settings.getString("PREF_EMAIL_ADDRESS", null);
		if (!sendMail || emailAddress == null) {
			return;
		}
				
		Uri uri = Uri.fromFile(new File(folder, filename));
		
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.emailBody));
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "[" + type + "] " + filename);
		sendIntent.putExtra(Intent.EXTRA_EMAIL,
				new String[] { emailAddress }); 
		sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
		sendIntent.setType("plain/txt");
		startActivityForResult(Intent.createChooser(sendIntent, getResources().getString(R.string.chooserTitle)), SELECT_PROGRAM);
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.arkiv_menu, menu);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case R.id.menuPreferences:
			startActivity(new Intent(this, Settings.class));
			break;
		case R.id.menuHelp:
		{
			AlertDialog helpDialog = new AlertDialog.Builder(this).create();
			Window w = helpDialog.getWindow();
			w.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			helpDialog.setTitle(R.string.helpTitle);
			helpDialog.setMessage(getResources().getText(R.string.helpText));
			helpDialog.show();
		}
		break;
		case R.id.menuAbout:
		{
			AlertDialog aboutDialog = new AlertDialog.Builder(this).create();
			Window w = aboutDialog.getWindow();
			w.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			aboutDialog.setTitle(R.string.aboutTitle);
			aboutDialog.setMessage(getResources().getText(R.string.aboutText));
			aboutDialog.show();
		}
		break;
		}
		return true;
	}
	
}