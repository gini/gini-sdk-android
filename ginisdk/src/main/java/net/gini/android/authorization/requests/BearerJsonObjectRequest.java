package net.gini.android.authorization.requests;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;

import net.gini.android.MediaTypes;
import net.gini.android.authorization.Session;

import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class BearerJsonObjectRequest extends JsonObjectRequest {
    final private Session mSession;

    public BearerJsonObjectRequest(int method, String url, JSONObject jsonRequest, Session session, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener, RetryPolicy retryPolicy) {
        super(method, url, jsonRequest, listener, errorListener);
        setRetryPolicy(retryPolicy);
        mSession = session;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", String.format("%s, %s", MediaTypes.APPLICATION_JSON, MediaTypes.GINI_JSON_V1));
        headers.put("Authorization", "BEARER " + mSession.getAccessToken());
        return headers;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            // The Gini API always uses UTF-8.
            final String jsonString = new String(response.data, HTTP.UTF_8);
            return Response.success(new JSONObject(jsonString),
                                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }
}
