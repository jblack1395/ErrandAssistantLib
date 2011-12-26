package com.jblack.android.errandassistantlib.activity;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListAdapter;

import com.jblack.android.errandassistantlib.adapter.DetailRowAdapter;
import com.jblack.android.errandassistantlib.provider.Road;
import com.jblack.android.errandrouterlib.R;

public class DetailTableViewActivity extends ListActivity {
	List<Road> tweetList = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = getIntent();
		tweetList = intent.getParcelableArrayListExtra("roads");
	}

	@Override
	protected void onResume() {
		super.onResume();
		ListAdapter adapter = new DetailRowAdapter(tweetList, this);
		getListView().setAdapter(adapter);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.details_menu, menu);

		return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        default:
            return super.onOptionsItemSelected(item);
        }
    }

}
