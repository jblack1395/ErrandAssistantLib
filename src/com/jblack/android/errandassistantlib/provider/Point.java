package com.jblack.android.errandassistantlib.provider;

import android.os.Parcel;
import android.os.Parcelable;

public class Point implements Parcelable {
    public String mName = "empty";
    public String mDescription = "empty";
    public String mIconUrl;
    public double mLatitude;
    public double mLongitude;
    
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mName);
        out.writeString(mDescription);
        out.writeDouble(mLatitude);
        out.writeDouble(mLongitude);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Point> CREATOR = new Parcelable.Creator<Point>() {
        public Point createFromParcel(Parcel in) {
            return new Point(in);
        }

        public Point[] newArray(int size) {
            return new Point[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private Point(Parcel in) {
        mName = in.readString();
        mDescription = in.readString();
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
    }

	public Point() {
		// TODO Auto-generated constructor stub
	}
}