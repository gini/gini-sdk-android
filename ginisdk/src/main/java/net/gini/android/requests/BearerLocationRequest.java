package net.gini.android.requests;


import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;

import net.gini.android.authorization.Session;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BearerLocationRequest extends JsonRequest<String> {
    private final String mAccessToken;

    public BearerLocationRequest(int method, String url, JSONObject jsonRequest,
                                 Session session,
                                 Response.Listener<String> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, (jsonRequest == null) ? null : jsonRequest.toString(), listener, errorListener);
        mAccessToken = session.getAccessToken();
    }

    @Override
    public Map<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "BEARER " + mAccessToken);
        return headers;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        final String header = response.headers.get("Location");
        return Response.success(header, HttpHeaderParser.parseCacheHeaders(response));
    }
}
