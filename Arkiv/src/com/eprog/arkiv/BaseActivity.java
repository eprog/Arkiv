package com.eprog.arkiv;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

public abstract class BaseActivity extends Activity {
	public static final int SELECT_PROGRAM = 0;
	public static final int SELECT_FROM_ARCHIVE = 1;
	private static final int CM_OPEN = 0;
	private static final int CM_RENAME = 1;
	protected String folder = null;
	protected String category = null;
	protected String filename = null;
	protected Uri selectedImageUri;
	protected ImageView imageView;
	protected SharedPreferences settings;
	protected View dialoglayout = null;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		menu.setHeaderTitle(R.string.contextMenuTitle);
		String desc = (String)v.getContentDescription();
		int order = Menu.NONE;
		if (desc.length() == 1) {
			order = Integer.parseInt(desc);
		}
		menu.add(0, CM_OPEN, order, getResources().getString(R.string.menuOpen));
		menu.add(0, CM_RENAME, order, getResources().getString(R.string.menuRename));
	}
	
	

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		
		switch (item.getItemId()) {

		case CM_OPEN:
			openCategoryFolder(item.getOrder());
			break;

		case CM_RENAME:
			renameCategory(item.getOrder());
			break;
		}
		return false;
	}
	
	private void openCategoryFolder(int order) {       
        Intent intent = new Intent(this, GalleryActivity.class);
		Bundle b = new Bundle();
		String folder = settings.getString(Settings.PREF_CATEGORY + order, "");
		if (folder == null || folder.length() == 0) {
			switch (order) {
			case 1:
				folder = getResources().getString(R.string.category1);
				break;
			case 2:
				folder = getResources().getString(R.string.category2);
				break;
			case 3:
				folder = getResources().getString(R.string.category3);
				break;
			case 4:
				folder = getResources().getString(R.string.category4);
				break;
			case 5:
				folder = getResources().getString(R.string.category5);
				break;
			case 6:
				folder = getResources().getString(R.string.category6);
				break;
			}
		}
		b.putString("folder", "/sdcard/Arkiv/" + folder);
		Log.d("Arkiv", "folder: " + b.getString("folder"));
		intent.putExtras(b);
		startActivity(intent);
	}

	private int categoryNr;
	protected Camera camera = null;
	
	// Let the user rename the category identified by the id
	private void renameCategory(int id) {
		
		this.categoryNr = id;
		
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
		
		// Get sub-category
		EditText text = (EditText)dialoglayout.findViewById(R.id.editCategory);
		String category = settings.getString(Settings.PREF_CATEGORY + categoryNr, "");
		if (category == null || category.length() == 0) {
			try {
				category = getResources().getString(getResources().getIdentifier(Settings.PREF_CATEGORY + categoryNr, "string", getPackageName()));
			} catch (Exception e) {
				Log.d("Arkiv", "Category not found.");
				category = ""; // TODO Get default category name
			}
		}
		text.setText(category);
		
		builder.setPositiveButton("OK", new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				Editor editor = settings.edit();
				// Get sub-category
				EditText text = (EditText)dialoglayout.findViewById(R.id.editCategory);
				Editable newCategory = text.getText();
				
				if (newCategory.length() > 0) {
					// Save new name for selected category
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
	public void setButtonLabels() {
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

	public boolean createFolder(String category) {
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
	
	public String getFolder(int category) {
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
		
	public void sendMail(String type, String folder, String filename) {
		
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
		case R.id.menuGallery:
			Intent intent = new Intent(this, GalleryActivity.class);
			Bundle b = new Bundle();
			b.putString("folder", "/sdcard/Arkiv");
			intent.putExtras(b);
			startActivity(intent);
			break;
		}
		return true;
	}
	
	public void showHelpDialog() {
		AlertDialog helpDialog = new AlertDialog.Builder(this).create();
		Window w = helpDialog.getWindow();
		w.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		helpDialog.setTitle(R.string.helpTitle);
		helpDialog.setMessage(getResources().getText(R.string.helpText));
		helpDialog.show();
	}



	protected void showSubCategoryDialog() {
	
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
				if (camera != null) {
					camera.startPreview();  // TODO Is this needed?
				}
			}
		});
		
		builder.show();
	}
		
}