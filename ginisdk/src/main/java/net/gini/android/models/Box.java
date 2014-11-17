package net.gini.android.models;


import org.json.JSONException;
import org.json.JSONObject;

public class Box {
    private final int mPageNumber;
    private final double mLeft;
    private final double mTop;
    private final double mWidth;
    private final double mHeight;


    public Box(final int pageNumber, final double left, final double top, final double width, final double height) {
        mPageNumber = pageNumber;
        mLeft = left;
        mTop = top;
        mWidth = width;
        mHeight = height;
    }

    public int getPageNumber() {
        return mPageNumber;
    }

    public double getLeft() {
        return mLeft;
    }

    public double getTop() {
        return mTop;
    }

    public double getWidth() {
        return mWidth;
    }

    public double getHeight() {
        return mHeight;
    }

    public static Box fromApiResponse(JSONObject responseData) throws JSONException {
        final int pageNumber = responseData.getInt("page");
        final double left = responseData.getDouble("left");
        final double top = responseData.getDouble("top");
        final double width = responseData.getDouble("width");
        final double height = responseData.getDouble("height");
        return new Box(pageNumber, left, top, width, height);
    }
}
