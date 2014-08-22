package net.gini.android.authorization.requests;


import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.Response;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


/**
 * Request to do a login request to the Gini User Center API in order to login the client.
 */
public class UserCenterLoginRequest extends JsonObjectRequest {

    final private String authorizationCredentials;

    public UserCenterLoginRequest(String clientId, String clientSecret, String baseUrl, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(Method.POST, baseUrl + "/oauth/token?grant_type=client_credentials", null, listener, errorListener);

        authorizationCredentials = Base64.encodeToString(String.format("%s:%s", clientId, clientSecret).getBytes(), Base64.NO_WRAP);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers =  new HashMap<String, String>();
        headers.put("Authorization", "Basic " + authorizationCredentials);
        headers.put("Accept", "application/json");
        return headers;
    }
}
