package com.jblack.android.errandassistantlib.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jblack.android.errandassistantlib.provider.Road;
import com.jblack.android.errandrouterlib.R;

public class DetailRowAdapter extends BaseAdapter {
	private List<Road> tweetList;

	private Context context;

	public DetailRowAdapter(List<Road> tweetList, Context context) {
		this.tweetList = tweetList;
		this.context = context;
	}

	public int getCount() {
		return tweetList.size();
	}

	public Road getItem(int position) {
		return tweetList.get(position);
	}

	public long getItemId(int position) {
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout itemLayout = null;
		Road tweet = tweetList.get(position);
		if (tweet == null)
			return null;
		itemLayout = (LinearLayout) LayoutInflater.from(context).inflate(
				R.layout.detail_row, parent, false);

		TextView tvUser = (TextView) itemLayout.findViewById(R.id.roadName);
		tvUser.setText(tweet.mName + "\r\n" + tweet.mDescription);

		TextView tvText = (TextView) itemLayout.findViewById(R.id.roadInfo);
		StringBuilder buf = new StringBuilder();
		for (int t = 0; t < tweet.mPoints.length; t++) {
			if (tweet.mPoints[t].mName != null
					|| tweet.mPoints[t].mName.trim().length() > 0) {
				if (!tweet.mPoints[t].mName.equals("empty")) {
					buf.append(tweet.mPoints[t].mName);
					if (!tweet.mPoints[t].mDescription.equals("empty"))
						buf.append(" ").append(tweet.mPoints[t].mDescription);
					buf.append("\r\n");
				}
			}
		}
		tvText.setBackgroundColor(tweet.mColor);
		tvText.setText(buf.toString());

		return itemLayout;
	}
}