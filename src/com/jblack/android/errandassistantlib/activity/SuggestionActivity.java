package com.jblack.android.errandassistantlib.activity;

import com.jblack.android.errandrouterlib.R;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SuggestionActivity extends CustomActivity {
	EditText suggestionText;
	Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.suggestion_view);
		context = this;
		final Button sendButton = (Button) findViewById(R.id.suggestion_email_button);
		sendButton.setEnabled(false);

		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent emailIntent = new Intent(
						android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
						new String[] { "planiturthian@gmail.com" });
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
						"Suggestion for Delivery Assistant");
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
						suggestionText.getText());
				startActivity(Intent
						.createChooser(emailIntent, "Send mail ..."));
				finish();
			}
		});
		suggestionText = (EditText) findViewById(R.id.suggestion);
		suggestionText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				sendButton.setEnabled(s.length() > 0);
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
		});
		final Button cancelButton = (Button) findViewById(R.id.suggestion_cancel_button);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
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

}
