package com.eprog.arkiv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ArkivActivity extends Activity implements SurfaceHolder.Callback {
	private static final int SELECT_PROGRAM = 0;
	private static final int SELECT_FROM_ARCHIVE = 1;
	private Camera camera = null;
	private String folder = null;
	private String category = null;
	private String filename = null;
	private Uri selectedImageUri;
	private ImageView imageView;
	private SharedPreferences settings;
	SurfaceView surface = null;
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

	

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		menu.setHeaderTitle(R.string.contextMenuTitle);
		String desc = (String)v.getContentDescription();
		int order = Menu.NONE;
		if (desc.length() == 1) {
			order = Integer.parseInt(desc);
		}
		MenuItem item = menu.add(0, v.getId(), order, getResources().getString(R.string.menuRename));
		
	}
	
	

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		
		renameCategory(item.getItemId(), item.getOrder());
		
		return false;
		
	}
	
	private int categoryNr;
	
	// Let the user rename the category identified by the id
	private void renameCategory(int id, int nr) {
		
		this.categoryNr = nr;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.menuRename);
		LayoutInflater inflater = getLayoutInflater();
		dialoglayout = inflater.inflate(R.layout.rename, (ViewGroup) getCurrentFocus());
		builder.setView(dialoglayout);
		AlertDialog dialog = builder.create();
		Window w = dialog.getWindow();
		w.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
//		TextView text = (TextView)dialoglayout.findViewById(R.id.renameDescription);
//		text.setText(R.string.subCategoryText);
		
		builder.setPositiveButton("OK", new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				Editor editor = settings.edit();
				// Get sub-category
				EditText text = (EditText)dialoglayout.findViewById(R.id.editCategory);
				Editable newCategory = text.getText();
				
				if (newCategory.length() > 0) {
					// TODO Save new name for selected category
					editor.putString(Settings.PREF_CATEGORY + categoryNr, newCategory.toString());
					editor.commit();
					// TODO Create category folder if it not exists
					setButtonLabels();
					Toast toast = Toast.makeText(getApplicationContext(), "New name: " + newCategory.toString(), Toast.LENGTH_SHORT);
					toast.show();
				} 				
			}
		});
		
		builder.show();
	}

	/**
	 * Set button labels if they have been modified by the user.
	 */
	private void setButtonLabels() {
		Button b = null;
		String label = null;
		String buttonID = null;
		int resID = 0;
		
		for (int i = 1; i < 7; i++) {
			buttonID = "buttonCategory" + i;
			resID = getResources().getIdentifier(buttonID, "id", "com.eprog.arkiv");

			label = settings.getString(Settings.PREF_CATEGORY + i, "");
			if (label != null && !label.equals("")) {
				b = (Button)findViewById(resID);
				b.setText(label);
			}
		}
	}

	private boolean createFolder(String category) {
		String folderName = settings.getString(category, "");
		
		if (folderName.equals("")) {
			
		}
		
    	File folder = new File(getResources().getString(R.string.rootPath) + folderName);
        boolean success = false;
        if(!folder.exists())
        {
            success = folder.mkdirs();
        }         
        return success;
    }
	
	private String getFolder(int category) {
		String folder = null;
		switch (category) {
		case 1:
			folder = settings.getString(Settings.PREF_CATEGORY1, getResources().getString(R.string.category1));
			break;
		case 2:
			folder = settings.getString(Settings.PREF_CATEGORY2, getResources().getString(R.string.category2));
			break;
		case 3:
			folder = settings.getString(Settings.PREF_CATEGORY3, getResources().getString(R.string.category3));
			break;
		case 4:
			folder = settings.getString(Settings.PREF_CATEGORY4, getResources().getString(R.string.category4));
			break;
		case 5:
			folder = settings.getString(Settings.PREF_CATEGORY5, getResources().getString(R.string.category5));
			break;
		case 6:
		default:
			folder = settings.getString(Settings.PREF_CATEGORY6, getResources().getString(R.string.category6));
			break;
		}
		return folder;
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
	
	private void sendMail(String type, String folder, String filename) {
		
		if (filename == null) {
			Log.d("Arkiv", "sendMail(): filename == null");
			return;
		}
		// Check if mail shall be sent
		Boolean sendMail = settings.getBoolean("PREF_SEND_MAIL", false);
		String emailAddress = settings.getString("PREF_EMAIL_ADDRESS", null);
		if (!sendMail || emailAddress == null) {
			return;
		}
		
        // Get sub-category
        String sub = settings.getString(Settings.PREF_SELCETED_SUB_CATEGORY, "");
				
		Uri uri = Uri.fromFile(new File(folder, filename));
		
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
			showHelpDialog();
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
	
	private void showHelpDialog() {
		AlertDialog helpDialog = new AlertDialog.Builder(this).create();
		Window w = helpDialog.getWindow();
		w.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		helpDialog.setTitle(R.string.helpTitle);
		helpDialog.setMessage(getResources().getText(R.string.helpText));
		helpDialog.show();
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
				sendMail(category, folder, filename);
	    		camera.startPreview();  // TODO Is this needed?
			}
		});
		
		builder.show();
	}
	
}