package com.jblack.android.errandassistantlib.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.Toast;

import com.jblack.android.errandrouterlib.R;

public class ErrandRouterActivity extends CustomActivity {

	private static final int REQUEST_CODE = 1234;
	static final int SEE_MAP_MENU = 1;
	static final int CLEAR_MENU = 2;
	protected List<Double> longitudeList = null;
	protected List<Double> latitudeList = null;

	public static final String PREFS_PRIVATE = "PREFS_PRIVATE";
	public static final String KEY_PRIVATE = "KEY_PRIVATE";
	protected Context mContext;
	private SharedPreferences prefsPrivate = null;

	public String[] getAddresses() {
		prefsPrivate = retrievePreferences();
		String s = prefsPrivate.getString("names", null);
		if (s == null)
			return null;
		String[] c = s.split(";");

		return c;
	}

	public double[] getLatitudeCoordinates() {
		prefsPrivate = retrievePreferences();
		String s = prefsPrivate.getString("latitude", null);
		if (s == null)
			return null;
		Log.i("CustomActivity", "getLatitudeCoordinates: " + s);
		String[] c = s.split(";");
		double[] ret = new double[c.length];
		for (int t = 0; t < c.length; t++) {
			ret[t] = Double.parseDouble(c[t]);
		}

		return ret;
	}

	public double[] getLongitudeCoordinates() {
		prefsPrivate = retrievePreferences();
		String s = prefsPrivate.getString("longitude", null);
		if (s == null)
			return null;
		Log.i("CustomActivity", "getLongitudeCoordinates: " + s);
		String[] c = s.split(";");
		double[] ret = new double[c.length];
		for (int t = 0; t < c.length; t++) {
			ret[t] = Double.parseDouble(c[t]);
		}

		return ret;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, requestCode + " RESULT_OK=" + (resultCode == RESULT_OK));
		if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
			// Populate the wordsList with the String values the recognition
			// engine thought it heard
			ArrayList<String> matches = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			for (String s : matches) {
				Log.i(TAG, s);
			}
			addressText.setText(matches.get(0));
		} else {
			Toast.makeText(mContext, R.string.voice_not_recognized,
					Toast.LENGTH_LONG);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		TAG = ErrandRouterActivity.class.getSimpleName();
		super.onCreate(savedInstanceState);

		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Intent myIntent = new Intent(
					Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivity(myIntent);
		}

		retrievePreferences();
		setContentView(R.layout.main);

		addressText = (EditText) findViewById(R.id.addressText);

		saveButton = (Button) findViewById(R.id.saveButton);
		saveButton.setEnabled(false);
		speakButton = (Button) findViewById(R.id.speakButton);

		tableLayout = (TableLayout) findViewById(R.id.queryTableLayout);

		plotRouteButton = (Button) findViewById(R.id.plotRouteButton);
		plotRouteButton.setEnabled(false);

		InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(addressText.getWindowToken(), 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// call the base class to include system menus
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.addroutes, menu);

		// It is important to return true to see the menu
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.see_map_menu) {
			Intent intent = new Intent();
			intent.setClassName("com.jblack.android.errandrouterplus.activity",
					"com.jblack.android.errandrouterplus.activity.ErrandMapActivity");
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.clear_menu) {
			addressList = new ArrayList<String>();
			latitudeList = new ArrayList<Double>();
			longitudeList = new ArrayList<Double>();
			tableLayout.removeAllViews();
			saveCoordinates(addressList.toArray(new String[0]),
					latitudeList.toArray(new Double[0]),
					longitudeList.toArray(new Double[0]));
			return true;
		}

		// for the rest
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i("TAG", "OnResume");
		String[] list = getAddresses();
		Log.i(TAG, "onResume: list null is " + (list == null));
		if (addressList == null) {
			addressList = new ArrayList<String>();
			longitudeList = new ArrayList<Double>();
			latitudeList = new ArrayList<Double>();
		}
		if (list == null || list.length == 0)
			return;

		longitudeList = convertDoubleArrayToList(getLongitudeCoordinates());
		latitudeList = convertDoubleArrayToList(getLatitudeCoordinates());
		if (addressList.size() < list.length)
			for (String s : list) {
				if (!addressList.contains(s)) {
					addressList.add(s);
					makeTagGUI(s, addressList.size() - 1);
				}
			}
		else
			for (String s : list) {
				if (!addressList.contains(s)) {
					addressList.add(s);
				}
			}
	}

	public SharedPreferences retrievePreferences() {
		prefsPrivate = mContext.getSharedPreferences(PREFS_PRIVATE,
				Context.MODE_PRIVATE);
		return prefsPrivate;
	}

	public void saveCoordinates(String[] list, Double[] latList,
			Double[] longList) {
		prefsPrivate = retrievePreferences();
		StringBuilder buf = new StringBuilder();
		for (String a : list) {
			if (a != null && a.length() > 1) {
				if (buf.length() > 1)
					buf.append(";");
				buf.append(a);
			}
		}
		StringBuilder buf2 = new StringBuilder();
		for (Double a : latList) {
			if (buf2.length() > 1)
				buf2.append(";");
			buf2.append(a);
		}
		StringBuilder buf3 = new StringBuilder();
		for (Double a : longList) {
			if (buf3.length() > 1)
				buf3.append(";");
			buf3.append(a);
		}
		Editor e = prefsPrivate.edit();
		try {
			if (buf.length() < 4 || buf2.length() < 4 || buf3.length() < 4) {
				e.remove("names");
				e.remove("latitude");
				e.remove("longitude");
			} else {
				e.putString("names", buf.toString());
				e.putString("latitude", buf2.toString());
				e.putString("longitude", buf3.toString());
			}
		} finally {
			e.commit();
		}
	}


	public void speakButtonClicked(View v) {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				"Voice recognition Demo...");
		startActivityForResult(intent, REQUEST_CODE);
	}
}