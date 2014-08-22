package net.gini.android.requests;


import android.test.AndroidTestCase;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;
import com.android.volley.toolbox.RequestFuture;

import net.gini.android.authorization.requests.UserCenterLoginRequest;
import net.gini.android.helpers.MockNetwork;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ExecutionException;


public class UserCenterLoginRequestTest extends AndroidTestCase {
    public RequestFuture<JSONObject> requestFuture;
    public UserCenterLoginRequest loginRequest;
    public RequestQueue requestQueue;
    public MockNetwork mockNetwork;

    @Override
    public void setUp() {
        requestFuture = RequestFuture.newFuture();
        loginRequest = new UserCenterLoginRequest("foobar", "1234", "https://user.gini.net/", requestFuture, requestFuture);
        mockNetwork = new MockNetwork();
        requestQueue = new RequestQueue(new NoCache(), mockNetwork);
        requestQueue.start();
    }

    public void testJSONAcceptHeader() throws AuthFailureError {
        Map<String, String> headers = loginRequest.getHeaders();

        assertEquals(headers.get("Accept"), "application/json");
    }

    public void testAuthorizationHeader() throws AuthFailureError {
        Map<String, String> headers = loginRequest.getHeaders();

        assertEquals(headers.get("Authorization"), "Basic Zm9vYmFyOjEyMzQ=");
    }

    public void testCorrectURL() {
        assertEquals(loginRequest.getUrl(), "https://user.gini.net//oauth/token?grant_type=client_credentials");
    }

    /** The login request has an empty Body */
    public void testEmptyBody() {
        assertEquals(loginRequest.getBody(), null);
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
