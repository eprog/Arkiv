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
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


public class SendActivity extends Activity {
	
	private static final int SELECT_PROGRAM = 0;
	private SharedPreferences settings;
	private Uri uri = null;
	private String category = null;
	private String folder = null;
	
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
                    
                    // TODO show image in the ImageView
                    //ImageView imageView = (ImageView)findViewById(R.id.sendImageView);
                    
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
    	// Check if subcategory dialog shall be shown
    	Boolean subCategories = settings.getBoolean("PREF_SUB_CATEGORIES", false);
  
		switch (view.getId()) {
		case R.id.buttonIntyg:
			if (subCategories) {
				folder = getResources().getString(R.string.folderIntyg);
				category = getResources().getString(R.string.buttonIntyg);
	    		showSubCategoryDialog(); 
			} else {
				copyAndSendMail(getResources().getString(R.string.folderIntyg), getResources().getString(R.string.buttonIntyg));
			}
			break;
		case R.id.buttonLedighet:
			if (subCategories) {
				folder = getResources().getString(R.string.folderLedighet);
				category = getResources().getString(R.string.buttonLedighet);
	    		showSubCategoryDialog(); 
			} else {
				copyAndSendMail(getResources().getString(R.string.folderLedighet), getResources().getString(R.string.buttonLedighet));
			}
			break;

		case R.id.buttonKvitto:
			if (subCategories) {
				folder = getResources().getString(R.string.folderKvitto);
				category = getResources().getString(R.string.buttonKvitto);
	    		showSubCategoryDialog(); 
			} else {
				copyAndSendMail(getResources().getString(R.string.folderKvitto), getResources().getString(R.string.buttonKvitto));
			}
			break;

		case R.id.buttonFaktura:
			if (subCategories) {
				folder = getResources().getString(R.string.folderFaktura);
				category = getResources().getString(R.string.buttonFaktura);
	    		showSubCategoryDialog(); 
			} else {
				copyAndSendMail(getResources().getString(R.string.folderFaktura), getResources().getString(R.string.buttonFaktura));
			}
			break;

		case R.id.buttonOther:
			if (subCategories) {
				folder = getResources().getString(R.string.folderOther);
				category = getResources().getString(R.string.buttonOther);
	    		showSubCategoryDialog(); 
			} else {
				copyAndSendMail(getResources().getString(R.string.folderOther), getResources().getString(R.string.buttonOther));
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
    
    private View dialoglayout = null;
	
	private void showSubCategoryDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.subCategoryTitle);
		LayoutInflater inflater = getLayoutInflater();
		dialoglayout = inflater.inflate(R.layout.subcategory, (ViewGroup) getCurrentFocus());
		builder.setView(dialoglayout);
		AlertDialog dialog = builder.create();
		Window w = dialog.getWindow();
		w.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		TextView text = (TextView)dialoglayout.findViewById(R.id.subCategoryDescription);
		text.setText(R.string.subCategoryText);
		
		Spinner spinCategory = (Spinner) dialoglayout.findViewById(R.id.subCategorySpinner);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		int count = settings.getInt(Settings.PREF_CATEGORY1_SUB_COUNT, 0);
		for (int i = 0; i < count; i++) {
			adapter.add(settings.getString(Settings.PREF_CATEGORY1_SUB + i, ""));
		}
		spinCategory.setAdapter(adapter);
		
		builder.setPositiveButton("OK", new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				Editor editor = settings.edit();
				// Get sub-category
				EditText text = (EditText)dialoglayout.findViewById(R.id.editSubCategory);
				Editable newCategory = text.getText();
				
				if (newCategory.length() > 0) {
					editor.putString(Settings.PREF_SELCETED_SUB_CATEGORY, newCategory.toString());
					// Save new category
					int count = settings.getInt(Settings.PREF_CATEGORY1_SUB_COUNT, 0);
					editor.putString(Settings.PREF_CATEGORY1_SUB + count, newCategory.toString());
					editor.putInt(Settings.PREF_CATEGORY1_SUB_COUNT, count + 1);
				} else {
					Spinner spinCategory = (Spinner) dialoglayout.findViewById(R.id.subCategorySpinner);
					editor.putString(Settings.PREF_SELCETED_SUB_CATEGORY, (String) spinCategory.getSelectedItem());
				}
				editor.commit();
				copyAndSendMail(folder, category);
			}
		});
		
		builder.show();
	}
}
