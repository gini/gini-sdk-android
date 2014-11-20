package net.gini.android.requests;

import com.android.volley.Response;

import net.gini.android.MediaTypes;
import net.gini.android.authorization.Session;

import java.util.HashMap;
import java.util.Map;


public class BearerUploadRequest extends BearerLocationRequest{
    private final byte[] mUploadData;
    private final String mContentType;
    private final String mAccessToken;

    public BearerUploadRequest(int method, String url, byte[] uploadData, String contentType,
                               final Session session,
                               Response.Listener<String> listener,
                               Response.ErrorListener errorListener) {
        super(method, url, null, session, listener, errorListener);

        mUploadData = uploadData;
        mContentType = contentType;
        mAccessToken = session.getAccessToken();
    }

    @Override
    public byte[] getBody() {
        return mUploadData;
    }

    @Override
    public Map<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", mContentType);
        headers.put("Accept", String.format("%s, %s", MediaTypes.APPLICATION_JSON, MediaTypes.GINI_JSON_V1));
        headers.put("Authorization", "BEARER " + mAccessToken);
        return headers;
    }
}
