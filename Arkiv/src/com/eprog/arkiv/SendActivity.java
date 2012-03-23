package com.eprog.arkiv;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
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
                    // Get resource path from intent callee
                    uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);

//                    // Query gallery for camera picture via
//                    // Android ContentResolver interface
//                    ContentResolver cr = getContentResolver();
//                    InputStream is = cr.openInputStream(uri);
//                    // Get binary bytes for encode
//                    byte[] data = getBytesFromFile(is);
//
//                    // base 64 encode for text transmission (HTTP)
//                    byte[] encoded_data = Base64.encodeBase64(data);
//                    String data_string = new String(encoded_data); // convert to string
//
//                    SendRequest(data_string);

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
			sendMail("Intyg", "Intyg");
			break;
		case R.id.buttonLedighet:
			sendMail("Ledighet", "Ledighet");
			break;

		case R.id.buttonKvitto:
			sendMail("Kvitto", "Kvitto");
			break;

		case R.id.buttonFaktura:
			sendMail("Faktura", "Faktura");
			break;

		case R.id.buttonOther:
			sendMail("Other", "Other");
			break;

//		case R.id.buttonLog:
//	        Intent intent = new Intent();
//            intent.setType("image/*");
//            intent.setAction(Intent.ACTION_GET_CONTENT);
//            startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.chooserTitle)), SELECT_FROM_ARCHIVE);
//			break;
		}
    }

    private void sendMail(String type, String folder) {
		// Check if mail shall be sent
		Boolean sendMail = settings.getBoolean("PREF_SEND_MAIL", false);
		String emailAddress = settings.getString("PREF_EMAIL_ADDRESS", null);
		if (!sendMail || emailAddress == null) {
			return;
		}
		
		Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String formattedDate = df.format(c.getTime());
        
		String dirPath = "/sdcard/Arkiv/" + folder;
        String filename = folder + formattedDate + ".jpg";
				
		//Uri uri = Uri.fromFile(new File(folder, filename));
		
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.emailBody));
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "[" + type + "] " + filename);
		sendIntent.putExtra(Intent.EXTRA_EMAIL,
				new String[] { emailAddress }); 
		sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
		sendIntent.setType("plain/txt");
		startActivityForResult(Intent.createChooser(sendIntent, getResources().getString(R.string.chooserTitle)), SELECT_PROGRAM);
	}

}
