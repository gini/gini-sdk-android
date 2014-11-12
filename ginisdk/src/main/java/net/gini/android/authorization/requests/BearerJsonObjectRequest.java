package net.gini.android.authorization.requests;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import net.gini.android.MediaTypes;
import net.gini.android.authorization.Session;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BearerJsonObjectRequest extends JsonObjectRequest {
    final private Session mSession;

    public BearerJsonObjectRequest(int method, String url, JSONObject jsonRequest, Session session, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);

        mSession = session;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", String.format("%s, %s", MediaTypes.APPLICATION_JSON, MediaTypes.GINI_JSON_V1));
        headers.put("Authorization", "BEARER " + mSession.getAccessToken());
        return headers;
    }
}
