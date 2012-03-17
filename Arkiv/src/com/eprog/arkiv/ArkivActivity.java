package com.eprog.arkiv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class ArkivActivity extends Activity implements SurfaceHolder.Callback {
	private Camera camera = null;
	private String folder = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        SurfaceView surface = (SurfaceView)findViewById(R.id.surface);
        SurfaceHolder holder = surface.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.setFixedSize(300, 200);
        
        // Create folders
        createFolder("/sdcard/Arkiv/Intyg");
        createFolder("/sdcard/Arkiv/Ledighet");
        createFolder("/sdcard/Arkiv/Kvitto");
        createFolder("/sdcard/Arkiv/Faktura");
        createFolder("/sdcard/Arkiv/Other");
    }
    
    @Override
	protected void onPause() {
		super.onPause();
		
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		SurfaceView surface = (SurfaceView)findViewById(R.id.surface);
        SurfaceHolder holder = surface.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.setFixedSize(300, 200);
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
			// Show folder "/sdcard/Arkiv" and make it possible to navigate to sub folders and view stored images.
			break;
		}
    }

	private void takePicture(String folder) {		
		this.folder = folder;
	
		camera.takePicture(shutterCallback, rawCallback, jpegCallback);
		
	}
	
	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			// TODO Do something?
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
		try {
			camera = Camera.open();
			camera.setPreviewDisplay(holder);
			camera.setDisplayOrientation(90);
			camera.startPreview();			
			camera.autoFocus(autoFocusCallback);
		} catch (IOException e) {
			Log.d("Arkiv", e.getMessage());
		}
	}
	
	AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {

		public void onAutoFocus(boolean arg0, Camera arg1) {
			// TODO Auto-generated method stub
			
		}
		
	};

	public void surfaceDestroyed(SurfaceHolder holder) {
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}
	
	private void sendMail(String type, String folder, String filename) {
		Uri uri = Uri.fromFile(new File(folder, filename));
		
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.emailBody));
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "[" + type + "] " + filename);
		sendIntent.putExtra(Intent.EXTRA_EMAIL,
				new String[] { "eckejonsson@gmail.com" }); // TODO Get address from settings
//		sendIntent.setType("message/rfc822");
		sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
		sendIntent.setType("plain/txt");
		startActivity(Intent.createChooser(sendIntent, "Title:")); 
	}
	
}