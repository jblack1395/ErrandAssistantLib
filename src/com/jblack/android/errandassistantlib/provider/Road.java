package com.jblack.android.errandassistantlib.provider;

import android.os.Parcel;
import android.os.Parcelable;

public class Road implements Parcelable {
    public String mName;
    public String mDescription;
    public int mColor;
    public int mWidth;
    public double[][] mRoute = new double[][] {};
    public Point[] mPoints = new Point[] {};
    
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mName);
        out.writeString(mDescription);
        out.writeInt(mColor);
        out.writeInt(mWidth);
        out.writeInt(mPoints.length);
        out.writeTypedArray(mPoints, flags);
        out.writeInt(mRoute.length); // store the length
        for (int i = 0; i < mRoute.length; i++) {
        	out.writeInt(mRoute[i].length);
            out.writeDoubleArray(mRoute[i]);
        }
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Road> CREATOR = new Parcelable.Creator<Road>() {
        public Road createFromParcel(Parcel in) {
            return new Road(in);
        }

        public Road[] newArray(int size) {
            return new Road[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private Road(Parcel in) {
        mName = in.readString();
        mDescription = in.readString();
        mColor = in.readInt();
        mWidth = in.readInt();
        int len = in.readInt();
        mPoints = new Point[len];
        in.readTypedArray(mPoints, Point.CREATOR);
        mRoute = new double[in.readInt()][]; 
        for (int i = 0; i < mRoute.length; i++) {
        	 mRoute[i] = new double[in.readInt()];
             in.readDoubleArray(mRoute[i]);
        }
    }

	public Road() {
		// TODO Auto-generated constructor stub
	}
}