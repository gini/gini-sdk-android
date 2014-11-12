package net.gini.android.authorization.requests;


import android.test.AndroidTestCase;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;
import com.android.volley.toolbox.RequestFuture;

import net.gini.android.MediaTypes;
import net.gini.android.helpers.MockNetwork;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class TokenRequestTest extends AndroidTestCase {
    public RequestFuture<JSONObject> requestFuture;
    public TokenRequest loginRequest;
    public RequestQueue requestQueue;
    public MockNetwork mockNetwork;

    @Override
    public void setUp() {
        requestFuture = RequestFuture.newFuture();
        loginRequest = new TokenRequest("foobar", "1234", "https://user.gini.net/oauth/token?grant_type=client_credentials", null, requestFuture, requestFuture);
        mockNetwork = new MockNetwork();
        requestQueue = new RequestQueue(new NoCache(), mockNetwork);
        requestQueue.start();
    }

    public void testJSONAcceptHeader() throws AuthFailureError {
        Map<String, String> headers = loginRequest.getHeaders();

        assertEquals(MediaTypes.APPLICATION_JSON, headers.get("Accept"));
    }

    public void testAuthorizationHeader() throws AuthFailureError {
        Map<String, String> headers = loginRequest.getHeaders();

        assertEquals("Basic Zm9vYmFyOjEyMzQ=", headers.get("Authorization"));
    }

    public void testCorrectURL() {
        assertEquals("https://user.gini.net/oauth/token?grant_type=client_credentials", loginRequest.getUrl());
    }

    public void testEmptyBody() {
        assertNull(loginRequest.getBody());
    }

    public void testBody() {
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("foo", "bar");
        data.put("bar", "foo");
        loginRequest = new TokenRequest("foobar", "1234", "https://user.gini.net", data, null, null);

        assertEquals("bar=foo&foo=bar", new String(loginRequest.getBody()));
    }

    public void testBodyContentTypeHeader() throws AuthFailureError {
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("foo", "bar");
        data.put("bar", "foo");
        loginRequest = new TokenRequest("foobar", "1234", "https://user.gini.net", data, null, null);

        assertEquals(MediaTypes.APPLICATION_FORM_URLENCODED, loginRequest.getBodyContentType());
    }

    public void testSuccessfulResponse() throws ExecutionException, InterruptedException, JSONException {
        NetworkResponse response = MockNetwork.createResponse(200, "{\"access_token\":\"74c1e7fe-e464-451f-a6eb-8f0998c46ff6\",\"token_type\":\"bearer\",\"expires_in\":3599}", null);
        mockNetwork.setResponseToReturn(response);

        requestQueue.add(loginRequest);

        JSONObject responseData = requestFuture.get();
        assertEquals(responseData.getInt("expires_in"), 3599);
        assertEquals(responseData.getString("access_token"), "74c1e7fe-e464-451f-a6eb-8f0998c46ff6");
        assertEquals(responseData.getString("token_type"), "bearer");
    }
}
