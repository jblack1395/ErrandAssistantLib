package com.jblack.android.errandassistantlib.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jblack.android.errandassistantlib.util.UIHelper;
import com.jblack.android.errandrouterlib.R;

/**
 * Activity that includes custom behavior shared across the application. For
 * example, bringing up a menu with the settings icon when the menu button is
 * pressed by the user and then starting the settings activity when the user
 * clicks on the settings icon.
 */
public abstract class CustomActivity extends Activity {
	protected List<String> addressList = null;
	protected TableLayout tableLayout;
	protected EditText addressText;
	protected String TAG;
	protected Button plotRouteButton = null;
	protected Button saveButton = null;
	protected LocationManager locationManager = null;
	protected Button speakButton = null;

	protected List<Double> convertDoubleArrayToList(double[] array) {
		List<Double> list = new ArrayList<Double>();
		for (Double d : array) {
			list.add(d);
		}

		return list;
	}

	protected double[] convertDoubleListToArray(List<Double> list) {
		double[] ret = new double[list.size()];
		for (int t = 0; t < list.size(); t++) {
			ret[t] = list.get(t);
		}
		return ret;
	}

	public void finish() {
		/*
		 * This can only invoked by the user or the app finishing the activity
		 * by navigating from the activity so the HOME key was not pressed.
		 */
		UIHelper.homeKeyPressed = false;
		super.finish();
	}

	protected void makeTagGUI(String addresslabel, int index) {
		// get a reference to the LayoutInflater service
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// inflate new_tag_view.xml to create new tag and edit Buttons

		View newTagView = inflater.inflate(R.layout.new_row_view, null);
		TextView tv = (TextView) newTagView.findViewById(R.id.newAddressLabel);
		tv.setText(addresslabel);
		if (index < tableLayout.getChildCount())
			tableLayout.addView(newTagView, index);
		else
			tableLayout.addView(newTagView);
		plotRouteButton.setEnabled(addressList.size() > 1);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings_menu, menu);

		/*
		 * Assume that the HOME key will be pressed next unless a navigation
		 * event by the user or the app occurs.
		 */
		UIHelper.homeKeyPressed = true;

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		if (item.getItemId() == R.id.details_menu) {
			return true;
		} else if (item.getItemId() == R.id.exit_menu) {
			finish();
			return true;
		} else if (item.getItemId() == R.id.help_menu) {
			Toast.makeText(this, "Type in errand locations, then look at map",
					Toast.LENGTH_LONG);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	public boolean onSearchRequested() {
		/*
		 * Disable the SEARCH key.
		 */
		return false;
	}

	public void onStart() {
		super.onStart();

		/*
		 * Check if the app was just launched. If the app was just launch ed
		 * then assume that the HOME key will be pressed next unless a
		 * navigation event by the user or the app occurs. Otherwise the user or
		 * the app navigated to this activity so the HOME key was not pressed.
		 */

		UIHelper.checkJustLaunced();
	}

	public void onStop() {
		super.onStop();

		/*
		 * Check if the HOME key was pressed. If the HOME key was pressed then
		 * the app will be killed. Otherwise the user or the app is navigating
		 * away from this activity so assume that the HOME key will be pressed
		 * next unless a navigation event by the user or the app occurs.
		 */
		UIHelper.checkHomeKeyPressed(true);
	}
}