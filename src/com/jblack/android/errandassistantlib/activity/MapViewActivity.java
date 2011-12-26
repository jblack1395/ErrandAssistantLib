package com.jblack.android.errandassistantlib.activity;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.jblack.android.errandassistantlib.adapter.DetailRowAdapter;
import com.jblack.android.errandassistantlib.provider.Road;
import com.jblack.android.errandassistantlib.provider.RoadProvider;
import com.jblack.android.errandrouterlib.R;

public abstract class MapViewActivity extends CustomMapActivity {
	@SuppressWarnings("rawtypes")
	private class DownloadFilesTask extends AsyncTask<Void, Integer, List[]> {

		protected List[] doInBackground(Void... params) {
			return downloadTaskBackground(params);
		}

		protected void onPostExecute(List[] lists) {
			downloadTaskPostExecute(lists);
			progDialog.dismiss();
		}

		protected void onPreExecute() {
			while (geoPoint == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			saveProximityAlertPoint();
			progDialog = ProgressDialog.show(mContext, "Processing ...",
					"Finding Routes", true, true);
			calculatingRoutes = true;
		}
	}

	class MapOverlay extends com.google.android.maps.Overlay {
		Road mRoad;
		ArrayList<GeoPoint> mPoints;
		int mColor;

		public MapOverlay(Road road, MapView mv, int color) {
			mRoad = road;
			mColor = color;
			if (road == null)
				return;
			Log.i(TAG, "route length = " + road.mRoute.length);
			Log.i(TAG, "route name = " + road.mName);
			Log.i(TAG, "description = " + road.mDescription);
			if (road.mRoute.length > 0) {
				mPoints = new ArrayList<GeoPoint>();
				for (int i = 0; i < road.mRoute.length; i++) {
					mPoints.add(new GeoPoint(
							(int) (road.mRoute[i][1] * 1000000),
							(int) (road.mRoute[i][0] * 1000000)));
				}
				int moveToLat = (mPoints.get(0).getLatitudeE6() + (mPoints.get(
						mPoints.size() - 1).getLatitudeE6() - mPoints.get(0)
						.getLatitudeE6()) / 2);
				int moveToLong = (mPoints.get(0).getLongitudeE6() + (mPoints
						.get(mPoints.size() - 1).getLongitudeE6() - mPoints
						.get(0).getLongitudeE6()) / 2);
				GeoPoint moveTo = new GeoPoint(moveToLat, moveToLong);

				MapController mapController = mv.getController();
				mapController.animateTo(moveTo);
				mapController.setZoom(17);
			}
		}

		@Override
		public boolean draw(Canvas canvas, MapView mv, boolean shadow, long when) {
			super.draw(canvas, mv, shadow);
			if (!shadow) { // 2

				Point point = new Point();
				mapView.getProjection().toPixels(geoPoint, point); // 3

				Bitmap bmp = BitmapFactory.decodeResource(getResources(),
						R.drawable.marker_default); // 4

				int x = point.x - bmp.getWidth() / 2; // 5

				int y = point.y - bmp.getHeight(); // 6

				canvas.drawBitmap(bmp, x, y, null); // 7

				Bitmap bmp1 = BitmapFactory.decodeResource(getResources(),
						R.drawable.layout1);
				Bitmap bmp2 = BitmapFactory.decodeResource(getResources(),
						R.drawable.layout2);
				if (!calculatingRoutes) {
					int c = curRoute + 1;
					for (int t = c; t < roadList.size(); t++) {
						Road r1 = roadList.get(t);
						if (r1 != null) {
							try {
								mapView.getProjection()
										.toPixels(
												new GeoPoint(
														(int) (r1.mPoints[0].mLatitude * 1E6),
														(int) (r1.mPoints[0].mLongitude * 1E6)),
												point);
								canvas.drawBitmap((t - c) % 2 == 0 ? bmp1
										: bmp2, point.x - bmp.getWidth() / 2,
										point.y - bmp.getHeight(), null);
							} catch (NullPointerException e) {
								System.out.println(e.toString());
							}
						} else {
							roadList.remove(t);
						}
					}
					bmp = BitmapFactory.decodeResource(getResources(),
							R.drawable.finish);
					try {
						List<Integer> li = numWayPointList;
						int nsize = li.size();
						int i = li.get(nsize - 1);
						com.jblack.android.errandassistantlib.provider.Point[] p = roadList
								.get(numWayPointList.size() - 1).mPoints;
						mapView.getProjection().toPixels(
								new GeoPoint((int) (p[i].mLatitude * 1E6),
										(int) (p[i].mLongitude * 1E6)), point);
						canvas.drawBitmap(bmp, point.x - bmp.getWidth() / 2,
								point.y - bmp.getHeight(), null);
					} catch (NullPointerException e) {
						Log.i(TAG, (roadList.size() - 1)
								+ " is null though size is " + roadList.size()
								+ "\r\n" + e.toString());
					}
				}
			} else {
				drawPath(mv, canvas);
			}
			return true;
		}

		public void drawPath(MapView mv, Canvas canvas) {
			int x1 = -1, y1 = -1, x2 = -1, y2 = -1;
			Paint paint = new Paint();
			paint.setColor(mColor);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(3);
			if (mPoints == null)
				return;
			for (int i = 0; i < mPoints.size(); i++) {
				Point point = new Point();
				mv.getProjection().toPixels(mPoints.get(i), point);
				x2 = point.x;
				y2 = point.y;
				if (i > 0) {
					canvas.drawLine(x1, y1, x2, y2, paint);
				}
				x1 = x2;
				y1 = y2;
			}
		}
	}

	/** this criteria will settle for less accuracy, high power, and cost */
	public static Criteria createCoarseCriteria() {

		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_COARSE);
		c.setAltitudeRequired(false);
		c.setBearingRequired(false);
		c.setSpeedRequired(false);
		c.setCostAllowed(true);
		c.setPowerRequirement(Criteria.POWER_HIGH);
		return c;

	}

	/** this criteria needs high accuracy, high power, and cost */
	public static Criteria createFineCriteria() {

		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_FINE);
		c.setAltitudeRequired(false);
		c.setBearingRequired(false);
		c.setSpeedRequired(false);
		c.setCostAllowed(true);
		c.setPowerRequirement(Criteria.POWER_HIGH);
		return c;

	}

	ProgressDialog progDialog = null;

	Map<String, Road> map;

	private MapView mapView;

	private static String TAG = MapViewActivity.class.getSimpleName();

	Location currentBestLocation = null;

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	List<Road> roadList = null;

	protected ListView listview = null;
	GeoPoint geoPoint = null;
	LocationListener GPSLocationListener = null,
			networkLocationListener = null;
	LocationManager locationManager = null;
	public int waypointcntr = 0;
	static public boolean waypointChanged = false;
	TextView description;
	public int curRoute = 0;
	private static final long MINIMUM_DISTANCE = 50;
	private static final long MINIMUM_DISTANCECHANGE_FOR_UPDATE = 0; // in
	private static final int NUM_LOW_ACCURACY_COUNT = 10;
	private int lowAccuracyCnt = NUM_LOW_ACCURACY_COUNT + 1;
	private TextView providerName;
	List<Double> latitudeList;
	List<Double> longitudeList;
	List<Integer> numWayPointList;
	private boolean calculatingRoutes = false;
	// Meters
	private static final long MINIMUM_TIME_BETWEEN_UPDATE = 1000; // in
	// Milliseconds
	private List<String> namelist;
	protected Context mContext;
	private static int[] setcolors = { Color.DKGRAY, Color.GREEN, Color.RED,
			Color.BLUE };
	LocationProvider low;
	LocationProvider high;
	public static final String PREFS_PRIVATE = "PREFS_PRIVATE";
	public static final String KEY_PRIVATE = "KEY_PRIVATE";
	static boolean startingPointAdded = false;
	private SharedPreferences prefsPrivate = null;

	private void calculateDistanceInitially(List<Double> latlist,
			List<Double> lonlist, int t) {
		Double x = null, y = null, z = null;
		if (latlist.size() == 0) {
			x = latitudeList.get(0);
			y = longitudeList.get(0);
		} else {
			double compLat = latlist.get(latlist.size() - 1);
			double compLon = lonlist.get(lonlist.size() - 1);
			Double h;
			int num = latitudeList.size();
			for (int i = 0; i < num; i++) {
				double m = latitudeList.get(i);
				double n = longitudeList.get(i);
				h = distanceBetweenPoints2(compLat, compLon, m, n);
				Log.i(TAG, "calculateDistanceInitially: h = " + h + " z = " + z);
				if (h > 0 && (z == null || h < z)) {
					if (!latlist.contains(m) && !lonlist.contains(n)) {
						x = m;
						y = n;
						z = h;
					}

				}
			}
		}
		latlist.add(x);
		lonlist.add(y);
		latitudeList.remove(x);
		longitudeList.remove(y);
	}

	private List<Double> convertDoubleArrayToList(double[] array) {
		List<Double> list = new ArrayList<Double>();
		if (array != null && array.length > 0) {
			for (Double d : array) {
				list.add(d);
			}
		}

		return list;
	}

	private Double distanceBetweenPoints2(Double fromLat, Double fromLon,
			Double toLat, Double toLon) {
		Double d = null;
		String key = fromLat.toString() + " " + fromLon.toString() + " "
				+ toLat.toString() + " " + toLon.toString();
		String ss;
		if (!map.containsKey(key)) {
			String url = RoadProvider.getUrl(fromLat, fromLon, toLat, toLon);
			InputStream is = null;
			while (is == null) {
				is = getConnection(url);
				if (is == null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			Road mRoad = RoadProvider.getRoute(is);
			if (mRoad.mDescription == null)
				return 999999999.0;
			String[] s = mRoad.mDescription.split(" ");
			if (s.length > 1) {
				Log.i(TAG, mRoad.mName + " " + s[1]);
				ss = s[1].replace("mi", "").replace("ft", "").trim();
				try {
					d = Double.parseDouble(ss);
					if (d > 0) {
						map.put(key, mRoad);
					}
				} catch (NumberFormatException e) {
					Log.d(TAG, "distanceBetweeenPoints: s[1] = " + s[1]
							+ " and error was " + e.toString());
				}
			}
		} else {
			Road r = map.get(key);
			String[] s = r.mDescription.split(" ");
			if (s.length > 1) {
				Log.i(TAG, r.mName + " " + s[1]);
				ss = s[1].replace("mi", "").replace("ft", "").trim();
				try {
					d = Double.parseDouble(ss);
					if (d > 0) {
						map.put(key, r);
					}
				} catch (NumberFormatException e) {
					Log.d(TAG, "distanceBetweeenPoints: s[1] = " + s[1]
							+ " and error was " + e.toString());
				}
			}
		}
		// Log.i(TAG, "distanceBetweenPoints: " + key + " = " + d);
		return d;
	}

	private List[] downloadTaskBackground(Void[] params) {
		namelist = new ArrayList<String>();
		List<Integer> colorlist = new ArrayList<Integer>();
		List<Road> roadlist = new ArrayList<Road>();
		if (!latitudeList.contains(geoPoint.getLatitudeE6() / 1E6))
			latitudeList.add(0, geoPoint.getLatitudeE6() / 1E6);
		if (!longitudeList.contains(geoPoint.getLongitudeE6() / 1E6))
			longitudeList.add(0, geoPoint.getLongitudeE6() / 1E6);
		int latnum = latitudeList.size();
		if (latitudeList != null && latnum > 0) {
			List<Double> latlist = new ArrayList<Double>();
			List<Double> lonlist = new ArrayList<Double>();
			map = new HashMap<String, Road>();
			for (int t = curRoute; t < latnum - 1; t++) {
				try {
					calculateDistanceInitially(latlist, lonlist, curRoute);
				} catch (IllegalArgumentException e) {
					Log.i(TAG,
							"calculateDistanceInitially error: " + e.toString()
									+ " for index " + curRoute);
					t = t - 1;
				}
			}
			latlist.add(latitudeList.get(0));
			latitudeList.remove(0);
			lonlist.add(longitudeList.get(0));
			longitudeList.remove(0);
			latitudeList = latlist;
			longitudeList = lonlist;
			for (int t = 1; t < latitudeList.size(); t++) {
				String key = latitudeList.get(t - 1).toString() + " "
						+ longitudeList.get(t - 1).toString() + " "
						+ latitudeList.get(t).toString() + " "
						+ longitudeList.get(t).toString();
				Road r = map.get(key);
				if (r != null)
					roadlist.add(r);
				else {
					distanceBetweenPoints2(latitudeList.get(t - 1),
							longitudeList.get(t - 1), latitudeList.get(t),
							longitudeList.get(t));
					roadlist.add(map.get(key));
				}
			}
		}
		numWayPointList = new ArrayList<Integer>();
		try {
			for (int t = 0; t < roadlist.size(); t++) {
				Road r = roadlist.get(t);
				if (r == null) {
					roadlist.remove(t);
				} else {
					for (int i = r.mPoints.length - 1; i > 0; i--) {
						if (r.mPoints[i] != null
								&& Math.abs(r.mPoints[i].mLatitude) > 0) {
							numWayPointList.add(i);
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return new List[] { namelist, colorlist, roadlist };
	}

	public void downloadTaskPostExecute(List[] lists) {
		@SuppressWarnings("unchecked")
		List<String> namelist = lists[0];
		@SuppressWarnings("unchecked")
		List<Road> roadlist = lists[2];

		// if (geoPoint == null)
		// return;
		Road r;
		namelist.clear();
		for (int t = 0; t < roadlist.size(); t++) {
			r = roadlist.get(t);
			if (r == null)
				continue;
			r.mColor = setcolors[t % setcolors.length];
			roadlist.set(t, r);
			for (int i = 0; i < r.mPoints.length; i++) {
				namelist.add(r.mPoints[i].mName + "\r\n"
						+ r.mPoints[i].mDescription);
			}
			MapOverlay mapOverlay = new MapOverlay(roadlist.get(t), mapView,
					setcolors[t % setcolors.length]);
			List<Overlay> listOfOverlays = mapView.getOverlays();
			listOfOverlays.add(mapOverlay);
		}
		calculatingRoutes = false;
		mapView.invalidate();
		@SuppressWarnings("unchecked")
		ListAdapter adapter = new DetailRowAdapter(lists[2], mContext);
		listview.setAdapter(adapter);
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
				Log.i(TAG, "converted to " + pos);
				String s = "http://maps.google.com/maps?saddr="
						+ latitudeList.get(pos) + "," + longitudeList.get(pos)
						+ "&daddr=" + latitudeList.get(pos + 1) + ","
						+ longitudeList.get(pos + 1);

				Log.i(TAG, s);
				Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
						Uri.parse(s));
				intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
						| Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.setClassName("com.google.android.apps.maps",
						"com.google.android.maps.MapsActivity");
				startActivity(intent);
			}
		});
		/*
		 * listview.setOnItemLongClickListener(new
		 * AdapterView.OnItemLongClickListener() {
		 * 
		 * @Override public boolean onItemLongClick(AdapterView<?> av, View v,
		 * int pos, long id) { return true; } });
		 */
		if (roadlist == null || roadlist.size() == 0)
			return;
		roadList = roadlist;
		geoPoint = new GeoPoint((int) (roadlist.get(0).mRoute[0][1] * 1E6),
				(int) (roadlist.get(0).mRoute[0][0] * 1E6));
		MapController mc = mapView.getController();
		mc.animateTo(geoPoint);
		// saveProximityAlertPoint();
		description.setText(namelist.get(waypointcntr + 1));
	}

	public String[] getAddresses() {
		prefsPrivate = retrievePreferences();
		String s = prefsPrivate.getString("names", null);
		if (s == null)
			return null;
		String[] c = s.split(";");

		return c;
	}

	private InputStream getConnection(String url) {
		InputStream is = null;
		try {
			URLConnection conn = new URL(url).openConnection();
			is = conn.getInputStream();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return is;
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

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		Log.i(TAG, "isBetterLocation - much newer = " + isSignificantlyNewer
				+ "  much older = " + isSignificantlyOlder + " isNewer = "
				+ isNewer);
		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());
		Log.i(TAG, "isBetterLocation - more accurate = " + isMoreAccurate
				+ "  less accurate = " + isLessAccurate
				+ " much less accurate = " + isSignificantlyLessAccurate
				+ " same provider = " + isFromSameProvider);

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	@Override
	protected boolean isLocationDisplayed() {
		return true;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return true;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	@Override
	public void onBackPressed() {
		Log.d("CDA", "onBackPressed Called");
		if (listview.getVisibility() == View.VISIBLE)
			listview.setVisibility(View.GONE);
		else
			super.onBackPressed();
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
		MapController mc = mapView.getController();
		mc.animateTo(geoPoint);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Acquire a reference to the system Location Manager
		Window w = this.getWindow(); // in Activity's onCreate() for instance
		w.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.mapview);
		listview = (ListView) findViewById(R.id.map_detail_list);
		listview.setVisibility(View.GONE);

		providerName = (TextView) findViewById(R.id.provider_name);
		description = (TextView) findViewById(R.id.description);
		description.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String s = "http://maps.google.com/maps?saddr="
						+ (geoPoint.getLatitudeE6() / 1E6) + ","
						+ (geoPoint.getLongitudeE6() / 1E6) + "&daddr="
						+ roadList.get(curRoute + 1).mPoints[0].mLatitude + ","
						+ roadList.get(curRoute + 1).mPoints[0].mLongitude;

				Log.i(TAG, s);
				Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
						Uri.parse(s));
				intent.setClassName("com.google.android.apps.maps",
						"com.google.android.maps.MapsActivity");
				startActivity(intent);
			}
		});
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		// mapView.setSatellite(true);

		// Define a listener that responds to location updates
		GPSLocationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location
				// provider.
				// makeUseOfNewLocation(location);
				Log.i(TAG, "GPS location listener - found location");
				if (isBetterLocation(location)) {
					processLocationChange(location);
				}
				if (currentBestLocation != null) {
					if ((int) (location.getAccuracy() - currentBestLocation
							.getAccuracy()) < 0
							&& lowAccuracyCnt > NUM_LOW_ACCURACY_COUNT) {
					}
					lowAccuracyCnt = 0;
				}
				MapController mc = mapView.getController();
				mc.animateTo(geoPoint);
				mapView.invalidate();
			}

			public void onProviderDisabled(String provider) {
				high = locationManager.getProvider(locationManager
						.getBestProvider(createFineCriteria(), true));
				try {
					locationManager.removeUpdates(GPSLocationListener);
				} catch (Exception e) {
				}
				locationManager.requestLocationUpdates(high.getName(),
						MINIMUM_TIME_BETWEEN_UPDATE /* ms */,
						MINIMUM_DISTANCECHANGE_FOR_UPDATE /* meters */,
						GPSLocationListener);
			}

			public void onProviderEnabled(String provider) {
				high = locationManager.getProvider(locationManager
						.getBestProvider(createFineCriteria(), true));
				try {
					locationManager.removeUpdates(GPSLocationListener);
				} catch (Exception e) {
				}
				locationManager.requestLocationUpdates(high.getName(),
						MINIMUM_TIME_BETWEEN_UPDATE /* ms */,
						MINIMUM_DISTANCECHANGE_FOR_UPDATE /* meters */,
						GPSLocationListener);
				providerName.setText(provider);
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}
		};

		// Define a listener that responds to location updates
		networkLocationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location
				// provider.
				// makeUseOfNewLocation(location);
				Log.i(TAG, "Network location listener - found location");
					processLocationChange(location);
				lowAccuracyCnt++;
				MapController mc = mapView.getController();
				mc.animateTo(geoPoint);
				mapView.invalidate();
			}

			public void onProviderDisabled(String provider) {
				low = locationManager.getProvider(locationManager
						.getBestProvider(createCoarseCriteria(), true));
				try {
					locationManager.removeUpdates(networkLocationListener);
				} catch (Exception e) {
				}
				locationManager.requestLocationUpdates(low.getName(),
						MINIMUM_TIME_BETWEEN_UPDATE /* ms */,
						MINIMUM_DISTANCECHANGE_FOR_UPDATE /* meters */,
						networkLocationListener);
			}

			public void onProviderEnabled(String provider) {
				low = locationManager.getProvider(locationManager
						.getBestProvider(createCoarseCriteria(), true));
				try {
					locationManager.removeUpdates(networkLocationListener);
				} catch (Exception e) {
				}
				locationManager.requestLocationUpdates(low.getName(),
						MINIMUM_TIME_BETWEEN_UPDATE /* ms */,
						MINIMUM_DISTANCECHANGE_FOR_UPDATE /* meters */,
						networkLocationListener);
				providerName.setText(provider);
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}
		};
		retrievePreferences();
	}

	private void processLocationChange(Location location) {
		if (calculatingRoutes)
			return;
		try {
			if (roadList == null || curRoute > roadList.size()
					|| roadList.get(curRoute) == null)
				return;
		} catch (NullPointerException e) {
			Log.i(TAG, "Threw exception, curRoute=" + curRoute);
			return;
		}
		Location loc2 = new Location("reverseGeocoded");
		com.jblack.android.errandassistantlib.provider.Point[] p = roadList
				.get(curRoute).mPoints;
		loc2.setLatitude(p[waypointcntr + 1].mLatitude);
		loc2.setLongitude(p[waypointcntr + 1].mLongitude);
			if (curRoute < roadList.size()) {
				Location loc = new Location("reverseGeocoded");
				loc.setLatitude(p[numWayPointList.get(curRoute)].mLatitude);
				loc.setLongitude(p[numWayPointList.get(curRoute)].mLongitude);
				if (location.distanceTo(loc) < MINIMUM_DISTANCE) {
					curRoute++;
					waypointcntr = 0;
					Log.i(TAG, "route changed to " + curRoute);
				} else if (location.distanceTo(loc2) < MINIMUM_DISTANCE) {
					Log.i(TAG,
							"waypoint changed to waypoint number "
									+ (waypointcntr + 1));
					waypointChanged = true;
				}
				providerName.setText("distance to next waypoint "
						+ "=" + location.distanceTo(loc2) + "(m)");
				providerName.invalidate();
			} else {
				curRoute = roadList.size() - 2;
			}
		if (namelist != null && namelist.size() > 0
				&& waypointcntr < namelist.size()) {
			description.setText(p[waypointcntr].mName + " " + p[waypointcntr].mDescription);
			description.invalidate();
			if (waypointChanged) {
				waypointcntr++;
				waypointChanged = false;
			}
		}
		if (currentBestLocation != null) {

		geoPoint = new GeoPoint(
				(int) (location.getLatitude() * 1E6),
				(int) (location.getLongitude() * 1E6));
		currentBestLocation = location;
			// longitudeList =
			// convertDoubleArrayToList(getLongitudeCoordinates());
			// latitudeList =
			// convertDoubleArrayToList(getLatitudeCoordinates());
			// latitudeList.add(0, location.getLatitude());
			// longitudeList.add(0, location.getLongitude());
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		// call the base class to include system menus
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		try {
			locationManager.removeUpdates(GPSLocationListener);
		} catch (Exception e) {
		}
		try {
			locationManager.removeUpdates(networkLocationListener);
		} catch (Exception e) {
		}
		locationManager = null;
		Log.i(TAG, "removed the location update listeners in onDestroy");
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		Intent intent;
		if(item.getItemId() == R.id.details_menu) {
			listview.setVisibility(listview.getVisibility() == View.VISIBLE ? View.GONE
					: View.VISIBLE);
			return true;
		} else if(item.getItemId() == R.id.suggestion_menu) {
			intent = new Intent();
			intent.setClassName(
					"com.jblack.android.errandassistantlib.activity",
					"com.jblack.android.errandassistantlib.activity.SuggestionActivity");
			startActivity(intent);
			return true;
		} else if(item.getItemId() == R.id.recalc_menu) {
			longitudeList = convertDoubleArrayToList(getLongitudeCoordinates());
			latitudeList = convertDoubleArrayToList(getLatitudeCoordinates());
			longitudeList.add(0, geoPoint.getLongitudeE6()/1E6);
			latitudeList.add(0, geoPoint.getLatitudeE6()/1E6);
			new DownloadFilesTask().execute();
			return true;
		} else if(item.getItemId() == R.id.next_destination_menu) {
			curRoute++;
			waypointcntr = 0;
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		// Remove the listener you previously added
		Log.i(TAG, "removed the location update listeners");
		try {
			locationManager.removeUpdates(GPSLocationListener);
		} catch (Exception e) {
		}
		try {
			locationManager.removeUpdates(networkLocationListener);
		} catch (Exception e) {
		}
		locationManager = null;
		super.onPause();
		// wl.release();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (locationManager == null) {
			locationManager = (LocationManager) this
					.getSystemService(Context.LOCATION_SERVICE);
		}
		boolean firstTime = geoPoint == null;
		Log.i(TAG, "Setting a new request for location updates");
		low = locationManager.getProvider(locationManager.getBestProvider(
				createCoarseCriteria(), true));
		high = locationManager.getProvider(locationManager.getBestProvider(
				createFineCriteria(), true));
		locationManager.requestLocationUpdates(high.getName(),
				MINIMUM_TIME_BETWEEN_UPDATE /* ms */,
				MINIMUM_DISTANCECHANGE_FOR_UPDATE /* meters */,
				GPSLocationListener);
		locationManager.requestLocationUpdates(low.getName(),
				MINIMUM_TIME_BETWEEN_UPDATE /* ms */,
				MINIMUM_DISTANCECHANGE_FOR_UPDATE /* meters */,
				networkLocationListener);
		locationManager.requestLocationUpdates(
				LocationManager.PASSIVE_PROVIDER,
				MINIMUM_TIME_BETWEEN_UPDATE /* ms */,
				MINIMUM_DISTANCECHANGE_FOR_UPDATE /* meters */,
				GPSLocationListener);
		saveProximityAlertPoint();
		if (firstTime) {
			longitudeList = convertDoubleArrayToList(getLongitudeCoordinates());
			latitudeList = convertDoubleArrayToList(getLatitudeCoordinates());
			new DownloadFilesTask().execute((Void) null);
		} else {
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

	private void saveProximityAlertPoint() {
		Location location = null;
		location = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Log.i(TAG, "saveProximityAlert - used GPS");

		if (location == null) {
			location = locationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			Log.i(TAG, "saveProximityAlert - used network");
			if (location == null) {
				return;
			}
		}
		if (!startingPointAdded) {
			startingPointAdded = true;
//			if (!latitudeList.contains(location.getLatitude()))
//				latitudeList.add(0, location.getLatitude());
//			if (!longitudeList.contains(location.getLongitude()))
//				longitudeList.add(0, location.getLongitude());
		}
		geoPoint = new GeoPoint((int) (location.getLatitude() * 1E6),
				(int) (location.getLongitude() * 1E6));
	}

	public void startDownloadTask() {
		new DownloadFilesTask().execute((Void) null);
	}
}
