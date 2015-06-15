package net.gini.android.authorization;

import android.net.Uri;

import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;

import net.gini.android.RequestTaskCompletionSource;
import net.gini.android.authorization.requests.BearerJsonObjectRequest;
import net.gini.android.authorization.requests.TokenRequest;
import net.gini.android.requests.BearerLocationRequest;
import net.gini.android.requests.RetryPolicyFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import bolts.Task;

import static com.android.volley.Request.Method.GET;
import static com.android.volley.Request.Method.POST;


/**
 * The UserCenterAPIManager is responsible for communication with the Gini User Center API. This includes handling HTTP
 * related error handling.
 */
public class UserCenterAPICommunicator {

    final private RequestQueue mRequestQueue;
    final private String mBaseUrl;
    final private String mClientId;
    final private String mClientSecret;
    final private RetryPolicyFactory mRetryPolicyFactory;

    public UserCenterAPICommunicator(final RequestQueue requestQueue, final String baseUrl,
                                     final String clientId, final String clientSecret,
                                     final RetryPolicyFactory retryPolicyFactory) {
        mRequestQueue = requestQueue;
        mBaseUrl = baseUrl;
        mClientId = clientId;
        mClientSecret = clientSecret;
        this.mRetryPolicyFactory = retryPolicyFactory;
    }

    /**
     * Logs in this client to the Gini User Center API. Uses the instance's client credentials to identify the client.
     *
     * Please note that this is used to login the client (in other words: the app) and not a Gini user. Please do not
     * mix the different sessions.
     *
     * @return A task which will resolve to a JSONObject representing the API's response, which is a valid access token
     * for the Gini User Center API.
     */
    public Task<JSONObject> loginClient() {
        // Build and execute the request.
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final String url = mBaseUrl + "oauth/token?grant_type=client_credentials";
        TokenRequest loginRequest =
                new TokenRequest(mClientId, mClientSecret, url, null, completionSource, completionSource,
                                 mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(loginRequest);

        return completionSource.getTask();
    }

    /**
     * Logs in a Gini user.
     *
     * @param userCredentials The user's credentials.
     * @return A task which will resolve to a JSONObject representing the API's response, which is a valid access token
     * for the Gini API.
     */
    public Task<JSONObject> loginUser(UserCredentials userCredentials) {
        // Build and execute the request.
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final String url = mBaseUrl + "oauth/token?grant_type=password";
        final HashMap<String, String> data = new HashMap<String, String>();
        data.put("username", userCredentials.getUsername());
        data.put("password", userCredentials.getPassword());
        TokenRequest loginRequest =
                new TokenRequest(mClientId, mClientSecret, url, data, completionSource, completionSource,
                                 mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(loginRequest);

        return completionSource.getTask();
    }

    /**
     * Creates a new Gini user.
     *
     * @param userCredentials           The user's credentials.
     * @param userCenterApiSession      A valid session to do requests to the Gini User Center API.
     *
     * @return                          A task which will resolve to a JSONObject representing the API's response, which
     *                                  is a user information.
     * @throws JSONException            If the user credentials can't be JSON serialized.
     */
    public Task<Uri> createUser(final UserCredentials userCredentials, Session userCenterApiSession)
            throws JSONException {

        final RequestTaskCompletionSource<Uri> completionSource = RequestTaskCompletionSource.newCompletionSource();
        final String url = mBaseUrl + "api/users";
        final JSONObject data = new JSONObject(){{
            put("email", userCredentials.getUsername());
            put("password", userCredentials.getPassword());
        }};
        BearerLocationRequest request =
                new BearerLocationRequest(POST, url, data, userCenterApiSession, completionSource,
                                          completionSource, mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(request);

        return completionSource.getTask();
    }

    public Task<JSONObject> getUserInfo(Uri userUri, Session userCenterApiSession) {
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final BearerJsonObjectRequest request =
                new BearerJsonObjectRequest(GET, userUri.toString(), null, userCenterApiSession, completionSource,
                                            completionSource, mRetryPolicyFactory.newRetryPolicy());

        mRequestQueue.add(request);
        return completionSource.getTask();
    }
}
