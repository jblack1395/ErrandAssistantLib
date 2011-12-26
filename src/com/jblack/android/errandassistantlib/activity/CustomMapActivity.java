package com.jblack.android.errandassistantlib.activity;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.maps.MapActivity;
import com.jblack.android.errandassistantlib.util.UIHelper;
import com.jblack.android.errandrouterlib.R;

/**
 * Activity that includes custom behavior shared across the application. For
 * example, bringing up a menu with the settings icon when the menu button is
 * pressed by the user and then starting the settings activity when the user
 * clicks on the settings icon.
 */
public abstract class CustomMapActivity extends MapActivity {

	public void onStart() {
		super.onStart();

		/*
		 * Check if the app was just launched. If the app was just launched then
		 * assume that the HOME key will be pressed next unless a navigation
		 * event by the user or the app occurs. Otherwise the user or the app
		 * navigated to this activity so the HOME key was not pressed.
		 */

		UIHelper.checkJustLaunced();
	}

	public void finish() {
		/*
		 * This can only invoked by the user or the app finishing the activity
		 * by navigating from the activity so the HOME key was not pressed.
		 */
		UIHelper.homeKeyPressed = false;
		super.finish();
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
		if (item.getItemId() == R.id.exit_menu) {
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
}