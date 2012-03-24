package com.eprog.arkiv;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

public class SendActivity extends Activity {
	private static final int SELECT_PROGRAM = 0;
	private SharedPreferences settings;
	private Uri uri = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send);
		
		settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String action = intent.getAction();

        // if this is from the share menu
        if (Intent.ACTION_SEND.equals(action))
        {
            if (extras.containsKey(Intent.EXTRA_STREAM))
            {
                try
                {
                    // Get resource path from intent 
                    uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                    return;
                } catch (Exception e)
                {
                    Log.e(this.getClass().getName(), e.toString());
                }

            } 
        }
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
	
    public void clickHandler(View view) {
		switch (view.getId()) {
		case R.id.buttonIntyg:
			copyAndSendMail("Intyg", "Intyg");
			break;
		case R.id.buttonLedighet:
			copyAndSendMail("Ledighet", "Ledighet");
			break;

		case R.id.buttonKvitto:
			copyAndSendMail("Kvitto", "Kvitto");
			break;

		case R.id.buttonFaktura:
			copyAndSendMail("Faktura", "Faktura");
			break;

		case R.id.buttonOther:
			copyAndSendMail("Other", "Other");
			break;

		}
		finish();
    }

    private void copyAndSendMail(String type, String folder) {
    	
    	Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String formattedDate = df.format(c.getTime());
		String dirPath = "/sdcard/Arkiv/" + folder;
        String filename = folder + formattedDate + ".jpg";
        
    	// Copy image to archive folder
    	// Query gallery for camera picture via
    	// Android ContentResolver interface
    	ContentResolver cr = getContentResolver();
    	InputStream is = null;
		try {
			is = cr.openInputStream(uri);
		} catch (FileNotFoundException e1) {
			Log.d("Arkiv", e1.toString());
		}
    	// Get binary bytes for encode
    	byte[] data = null;
		try {
			data = getBytesFromFile(is);
		} catch (IOException e1) {
			Log.d("Arkiv", e1.toString());
		}
    	
    	// Save the image to the right folder on the SD card
    	FileOutputStream outStream = null;
    	try {
    		outStream = new FileOutputStream(dirPath + "/" + filename);
    		outStream.write(data);
    		outStream.close();
    	} catch (FileNotFoundException e) {
    		Log.d("Arkiv", e.getMessage());
    	} catch (IOException e) {
    		Log.d("Arkiv", e.getMessage());
    	}
    	
		// Check if mail shall be sent
		Boolean sendMail = settings.getBoolean("PREF_SEND_MAIL", false);
		String emailAddress = settings.getString("PREF_EMAIL_ADDRESS", null);
		if (!sendMail || emailAddress == null) {
			return;
		}
				
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.emailBody));
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "[" + type + "] " + filename);
		sendIntent.putExtra(Intent.EXTRA_EMAIL,
				new String[] { emailAddress }); 
		sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
		sendIntent.setType("plain/txt");
		startActivityForResult(Intent.createChooser(sendIntent, getResources().getString(R.string.chooserTitle)), SELECT_PROGRAM);
	}

    private static byte[] getBytesFromFile(InputStream ios) throws IOException {
    	ByteArrayOutputStream ous = null;

    	try {
    		byte[] buffer = new byte[4096];
    		ous = new ByteArrayOutputStream();
    		int read = 0;
    		while ((read = ios.read(buffer)) != -1) {
    			ous.write(buffer, 0, read);
    		}
    	} finally {
    		try {
    			if (ous != null)
    				ous.close();
    		} catch (IOException e) {
    			// swallow, since not that important
    		}
    		try {
    			if (ios != null)
    				ios.close();
    		} catch (IOException e) {
    			// swallow, since not that important
    		}
    	}
    	return ous.toByteArray();
    }

}
