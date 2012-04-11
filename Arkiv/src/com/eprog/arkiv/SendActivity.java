package com.eprog.arkiv;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class SendActivity extends BaseActivity {
	
	private Uri uri = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send);
		// Avoid screen rotation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
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
                    
                    // TODO show image in the ImageView
                    //ImageView imageView = (ImageView)findViewById(R.id.sendImageView);
                    
                    return;
                } catch (Exception e)
                {
                    Log.e(this.getClass().getName(), e.toString());
                }

            } 
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
//        	b.getBackground().setAlpha(45);
        }
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		setButtonLabels();
	}
	
    public void clickHandler(View view) {
    	// Check if subcategory dialog shall be shown
    	Boolean subCategories = settings.getBoolean("PREF_SUB_CATEGORIES", false);
  
		switch (view.getId()) {
		case R.id.buttonIntyg:
			if (subCategories) {
				folder = getResources().getString(R.string.folderIntyg);
				category = getResources().getString(R.string.buttonCategory1);
	    		showSubCategoryDialog(); 
			} else {
				copyAndSendMail(getResources().getString(R.string.folderIntyg), getResources().getString(R.string.buttonCategory1));
			}
			break;
		case R.id.buttonLedighet:
			if (subCategories) {
				folder = getResources().getString(R.string.folderLedighet);
				category = getResources().getString(R.string.buttonCategory2);
	    		showSubCategoryDialog(); 
			} else {
				copyAndSendMail(getResources().getString(R.string.folderLedighet), getResources().getString(R.string.buttonCategory2));
			}
			break;

		case R.id.buttonKvitto:
			if (subCategories) {
				folder = getResources().getString(R.string.folderKvitto);
				category = getResources().getString(R.string.buttonCategory3);
	    		showSubCategoryDialog(); 
			} else {
				copyAndSendMail(getResources().getString(R.string.folderKvitto), getResources().getString(R.string.buttonCategory3));
			}
			break;

		case R.id.buttonFaktura:
			if (subCategories) {
				folder = getResources().getString(R.string.folderFaktura);
				category = getResources().getString(R.string.buttonCategory4);
	    		showSubCategoryDialog(); 
			} else {
				copyAndSendMail(getResources().getString(R.string.folderFaktura), getResources().getString(R.string.buttonCategory4));
			}
			break;

		case R.id.buttonOther:
			if (subCategories) {
				folder = getResources().getString(R.string.folderOther);
				category = getResources().getString(R.string.buttonCategory5);
	    		showSubCategoryDialog(); 
			} else {
				copyAndSendMail(getResources().getString(R.string.folderOther), getResources().getString(R.string.buttonCategory5));
			}
			break;

		}
    }

    private void copyAndSendMail(String folder, String type) {
    	Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String formattedDate = df.format(c.getTime());
        String filename = type + formattedDate + ".jpg";
        
    	// Copy image to archive folder
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
    		outStream = new FileOutputStream(folder + "/" + filename);
    		outStream.write(data);
    		outStream.close();
    	} catch (FileNotFoundException e) {
    		Log.d("Arkiv", e.getMessage());
    	} catch (IOException e) {
    		Log.d("Arkiv", e.getMessage());
    	}
    	
    	// Insert image into Media Store
    	ContentValues content = new ContentValues(1);
    	content.put(Images.Media.MIME_TYPE, "image/jpg");
    	content.put(MediaStore.Images.Media.DATA, folder + "/" + filename);
    	
    	ContentResolver resolver = getContentResolver();
    	Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, content);
    	sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
    	
		// Check if mail shall be sent
		Boolean sendMail = settings.getBoolean("PREF_SEND_MAIL", false);
		String emailAddress = settings.getString("PREF_EMAIL_ADDRESS", null);
		if (!sendMail || emailAddress == null) {
			return;
		}
		
		// Get sub-category
        String sub = settings.getString(Settings.PREF_SELCETED_SUB_CATEGORY, "");
				
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.emailBody));
		if (sub != null && !sub.equals("")) {
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, "[" + type + "] [" + sub + "] " + filename);
		} else {
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, "[" + type + "] " + filename);
		}
		sendIntent.putExtra(Intent.EXTRA_EMAIL,
				new String[] { emailAddress }); 
		sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
		sendIntent.setType("plain/txt");
		startActivityForResult(Intent.createChooser(sendIntent, getResources().getString(R.string.chooserTitle)), SELECT_PROGRAM);
		finish();
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
