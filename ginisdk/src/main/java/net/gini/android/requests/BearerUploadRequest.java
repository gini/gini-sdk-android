package net.gini.android.requests;

import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import java.util.HashMap;
import java.util.Map;


public class BearerUploadRequest extends JsonObjectRequest{
    private final byte[] mUploadData;
    private final String mContentType;
    private final String mAccessToken;

    public BearerUploadRequest(int method, String url, byte[] uploadData, String contentType,
                               final String accessToken,
                               Response.Listener<org.json.JSONObject> listener,
                               Response.ErrorListener errorListener) {
        super(method, url, null, listener, errorListener);

        mUploadData = uploadData;
        mContentType = contentType;
        mAccessToken = accessToken;
    }

    @Override
    public byte[] getBody() {
        return mUploadData;
    }

    @Override
    public Map<String, String> getHeaders() {
        return new HashMap<String, String>() {{
            put("Content-Type", mContentType);
            put("Accept", "application/json,application/vnd.gini.v1+json");
            put("Authorization", "Bearer " + mAccessToken);
        }};
    }
}
