package net.gini.android.authorization;


import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;

import java.util.concurrent.Future;

import net.gini.android.ConstantFuture;

import net.gini.android.authorization.requests.UserCenterLoginRequest;

import org.json.JSONObject;


public class UserCenterManager {
    /** The RequestQueue instance which is used to do the requests to the Gini User Center API */
    private final RequestQueue mRequestQueue;
    /** The application's client ID for the Gini API. */
    private final String mClientId;
    /** The application's client secret for the Gini API */
    private final String mClientSecret;
    private final String mBaseUrl;

    // An active session for the User Center API.
    private Session mCurrentSession;

    /**
     * @param requestQueue      The RequestQueue instance (volley) which is used to do the requests
     *                          to the Gini User Center API.
     * @param clientId          The application's client ID for the Gini API.
     * @param clientSecret      The application's client secret for the Gini API.
     */
    public UserCenterManager(final RequestQueue requestQueue, final String baseUrl, final String clientId, final String clientSecret) {
        mRequestQueue = requestQueue;
        mClientId = clientId;
        mClientSecret = clientSecret;
        mBaseUrl = baseUrl;
    }

    /**
     * Creates a new user which has the given client credentials.
     *
     * @param userCredentials   The user's credentials.
     * @return                  TODO
     */
    public Future<User> createUser(UserCredentials userCredentials) {
        return null;
    }

    /**
     * Log-in the user  which is identified with the given credentials.
     *
     * TODO: explanation to not mix the sessions.
     *
     * @return                  TODO
     */
    public Future<Session> loginUser(UserCredentials userCredentials) {
        return null;
    }

    /**
     * Returns a future that will resolve to a valid session for the User Center API! (Not the user.
     */
    private Future<Session> getSession() {
        if (mCurrentSession != null && !mCurrentSession.hasExpired()) {
            return new ConstantFuture<Session>(mCurrentSession);
        }

        return login();
    }

    private Future<Session> login() {
        RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();
        UserCenterLoginRequest loginRequest = new UserCenterLoginRequest(mClientId, mClientSecret, mBaseUrl, requestFuture, requestFuture);
        mRequestQueue.add(loginRequest);

        return Session.sessionFutureFromAPIResponse(requestFuture);
    }
}
