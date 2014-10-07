package net.gini.android.authorization;

import com.android.volley.RequestQueue;

import net.gini.android.RequestTaskCompletionSource;
import net.gini.android.authorization.requests.TokenRequest;

import org.json.JSONObject;

import java.util.HashMap;

import bolts.Task;


/**
 * The UserCenterAPIManager is responsible for communication with the Gini User Center API. This
 * includes handling HTTP related error handling.
 */
public class UserCenterAPICommunicator {
    final private RequestQueue mRequestQueue;
    final private String mBaseUrl;
    final private String mClientId;
    final private String mClientSecret;

    UserCenterAPICommunicator(final RequestQueue requestQueue, final String baseUrl,
                              final String clientId, final String clientSecret) {
        mRequestQueue = requestQueue;
        mBaseUrl = baseUrl;
        mClientId = clientId;
        mClientSecret = clientSecret;
    }

    /**
     * Logs in this client to the Gini User Center API. Uses the instance's client credentials to
     * identify the client.
     *
     * Please note that this is used to login the client (in other words: the app) and not a Gini
     * user. Please do not mix the different sessions.
     *
     * @return                 A task which will resolve to a JSONObject representing the API's
     *                         response, which is a valid access token for the Gini User Center API.
     */
    public Task<JSONObject> loginClient() {
        // Build and execute the request.
        final RequestTaskCompletionSource<JSONObject> completionSource = RequestTaskCompletionSource.newTask();
        final String url = mBaseUrl + "oauth/token?grant_type=client_credentials";
        TokenRequest loginRequest = new TokenRequest(mClientId, mClientSecret, url, null, completionSource, completionSource);
        mRequestQueue.add(loginRequest);

        return completionSource.getTask();
    }

    /**
     * Logs in a Gini user.
     *
     * @param userCredentials  The user's credentials.
     * @return                 A task which will resolve to a JSONObject representing the API's
     *                         response, which is a valid access token for the Gini API.
     */
    public Task<JSONObject> loginUser(UserCredentials userCredentials) {
        // Build and execute the request.
        final RequestTaskCompletionSource<JSONObject> completionSource = RequestTaskCompletionSource.newTask();
        final String url = mBaseUrl + "oauth/token?grant_type=password";
        final HashMap<String, String> data = new HashMap<String, String>();
        data.put("username", userCredentials.getUsername());
        data.put("password", userCredentials.getPassword());
        TokenRequest loginRequest = new TokenRequest(mClientId, mClientSecret, url, data, completionSource, completionSource);
        mRequestQueue.add(loginRequest);

        return completionSource.getTask();
    }
}
