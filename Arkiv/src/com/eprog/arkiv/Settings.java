package com.eprog.arkiv;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity {
	
	// Category keys
	public static final String PREF_CATEGORY = "PREF_CATEGORY";
	public static final String PREF_CATEGORY1 = "PREF_CATEGORY1";
	public static final String PREF_CATEGORY2 = "PREF_CATEGORY2";
	public static final String PREF_CATEGORY3 = "PREF_CATEGORY3";
	public static final String PREF_CATEGORY4 = "PREF_CATEGORY4";
	public static final String PREF_CATEGORY5 = "PREF_CATEGORY5";
	public static final String PREF_CATEGORY6 = "PREF_CATEGORY6";

	public static final String PREF_CATEGORY1_SUB = "SUB_CATEGORY1";
	public static final String PREF_CATEGORY2_SUB = "SUB_CATEGORY2";
	public static final String PREF_CATEGORY3_SUB = "SUB_CATEGORY3";
	public static final String PREF_CATEGORY4_SUB = "SUB_CATEGORY4";
	public static final String PREF_CATEGORY5_SUB = "SUB_CATEGORY5";

	public static final String PREF_CATEGORY1_SUB_COUNT = "SUB_CATEGORY1_COUNT";
	public static final String PREF_CATEGORY2_SUB_COUNT = "SUB_CATEGORY2_COUNT";
	public static final String PREF_CATEGORY3_SUB_COUNT = "SUB_CATEGORY3_COUNT";
	public static final String PREF_CATEGORY4_SUB_COUNT = "SUB_CATEGORY4_COUNT";
	public static final String PREF_CATEGORY5_SUB_COUNT = "SUB_CATEGORY5_COUNT";

	public static final String PREF_SELCETED_SUB_CATEGORY = "SELECTED_SUB_CATEGORY";
	
	public static final String PREF_FIRST_START = "FIRST_START";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
	}
	
	

}
