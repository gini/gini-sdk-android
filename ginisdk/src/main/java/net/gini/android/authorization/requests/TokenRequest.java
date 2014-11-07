package net.gini.android.authorization.requests;


import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.Response;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static net.gini.android.Utils.mapToUrlEncodedString;


/**
 * Request to do a login request to the Gini User Center API in order to login the client.
 */
public class TokenRequest extends JsonObjectRequest {

    private final String mAuthorizationCredentials;
    private final Map<String, String> mRequestData;

    public TokenRequest(String clientId, String clientSecret, String url, @Nullable Map<String, String> requestData,
                        Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(Method.POST, url, null, listener, errorListener);

        mAuthorizationCredentials =
                Base64.encodeToString(String.format("%s:%s", clientId, clientSecret).getBytes(), Base64.NO_WRAP);
        mRequestData = requestData;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Basic " + mAuthorizationCredentials);
        headers.put("Accept", "application/json");
        return headers;
    }

    @Override
    public String getBodyContentType() {
        return "application/x-www-form-urlencoded";
    }

    @Override
    public byte[] getBody() {
        byte[] body = null;
        if (mRequestData != null) {
            body = mapToUrlEncodedString(mRequestData).getBytes();
        }
        return body;
    }
}
