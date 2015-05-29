package net.gini.android;


import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import net.gini.android.authorization.AnonymousSessionManager;
import net.gini.android.authorization.CredentialsStore;
import net.gini.android.authorization.SessionManager;
import net.gini.android.authorization.SharedPreferencesCredentialsStore;
import net.gini.android.authorization.UserCenterAPICommunicator;
import net.gini.android.authorization.UserCenterManager;

import static net.gini.android.Utils.checkNotNull;

public class SdkBuilder {

    private final Context mContext;

    private String mApiBaseUrl = "https://api.gini.net/";
    private String mUserCenterApiBaseUrl = "https://user.gini.net/";

    private String mEmailDomain;
    private String mClientId;
    private String mClientSecret;

    private ApiCommunicator mApiCommunicator;
    private RequestQueue mRequestQueue;
    private DocumentTaskManager mDocumentTaskManager;
    private SessionManager mSessionManager;
    private CredentialsStore mCredentialsStore;
    private UserCenterManager mUserCenterManager;
    private UserCenterAPICommunicator mUserCenterApiCommunicator;

    /**
     * Constructor to initialize a new builder instance where anonymous Gini users are used. <b>This requires access to
     * the Gini User Center API. Access to the User Center API is restricted to selected clients only.</b>
     *
     * @param context               Your application's Context instance (Android).
     * @param clientId              Your application's client ID for the Gini API.
     * @param clientSecret          Your application's client secret for the Gini API.
     * @param emailDomain           The email domain which is used for created Gini users.
     */
    public SdkBuilder(final Context context, final String clientId, final String clientSecret,
                      final String emailDomain) {
        mContext = context;
        mEmailDomain = emailDomain;
        mClientSecret = clientSecret;
        mClientId = clientId;
    }

    /**
     * Constructor to initialize a new builder instance. The created Gini instance will use the given
     * {@link SessionManager} for session management.
     *
     * @param context               Your application's Context instance (Android).
     * @param sessionManager        The SessionManager to use.
     */
    public SdkBuilder(final Context context, final SessionManager sessionManager) {
        mContext = context;
        mSessionManager = sessionManager;
    }

    /**
     * Set the base URL of the Gini API. Handy for tests. <b>Usually, you do not use this method</b>.
     *
     * @param newUrl                The URL of the Gini API which is used by the requests of the Gini SDK.
     * @return                      The builder instance to enable chaining.
     */
    public SdkBuilder setApiBaseUrl(String newUrl) {
        if (!newUrl.endsWith("/")) {
            newUrl += "/";
        }
        mApiBaseUrl = newUrl;
        return this;
    }

    /**
     * Set the base URL of the Gini User Center API. Handy for tests. <b>Usually, you do not use this method</b>.
     *
     * @param newUrl                The URL of the Gini User Center API which is used by the requests of the Gini SDK.
     * @return                      The builder instance to enable chaining.
     */
    public SdkBuilder setUserCenterApiBaseUrl(String newUrl) {
        if (!newUrl.endsWith("/")) {
            newUrl += "/";
        }
        mUserCenterApiBaseUrl = newUrl;
        return this;
    }

    /**
     * Set the credentials store which is used by the Gini SDK to store user credentials. If no credentials store is
     * set, the net.gini.android.authorization.SharedPreferencesCredentialsStore is used by default.
     *
     * @param credentialsStore      A credentials store instance (specified by the CredentialsStore interface).
     * @return                      The builder instance to enable chaining.
     */
    public SdkBuilder setCredentialsStore(CredentialsStore credentialsStore) {
        mCredentialsStore = checkNotNull(credentialsStore);
        return this;
    }

    /**
     * Builds the Gini instance with the configuration settings of the builder instance.
     *
     * @return                      The fully configured Gini instance.
     */
    public Gini build() {
        return new Gini(getDocumentTaskManager(), getCredentialsStore());
    }

    /**
     * Helper method to create (and store) the RequestQueue which is used for both the requests to the Gini API and the
     * Gini User Center API.
     *
     * @return                      The RequestQueue instance.
     */
    private synchronized RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        return mRequestQueue;
    }

    /**
     * Helper method to create (and store) the ApiCommunicator instance which is used to do the requests to the Gini API.
     *
     * @return                      The ApiCommunicator instance.
     */
    private synchronized ApiCommunicator getApiCommunicator() {
        if (mApiCommunicator == null) {
            mApiCommunicator = new ApiCommunicator(mApiBaseUrl, getRequestQueue());
        }
        return mApiCommunicator;
    }

    /**
     * Helper method to create (and store) the instance of the CredentialsStore implementation which is used to store
     * user credentials. If the credentials store was previously configured via the builder, the previously configured
     * instance is used. Otherwise, a net.gini.android.authorization.SharedPreferencesCredentialsStore instance is
     * created by default.
     *
     * @return                      The CredentialsStore instance.
     */
    private synchronized CredentialsStore getCredentialsStore() {
        if (mCredentialsStore == null) {
            SharedPreferences sharedPreferences = mContext.getSharedPreferences("Gini", Context.MODE_PRIVATE);
            mCredentialsStore = new SharedPreferencesCredentialsStore(sharedPreferences);
        }
        return mCredentialsStore;
    }

    /**
     * Helper method to create (and store) the UserCenterApiCommunicator instance which is used to do the requests to
     * the Gini User Center API.
     *
     * @return                      The ApiCommunicator instance.
     */
    private synchronized UserCenterAPICommunicator getUserCenterAPICommunicator() {
        if (mUserCenterApiCommunicator == null) {
            mUserCenterApiCommunicator =
                    new UserCenterAPICommunicator(getRequestQueue(), mUserCenterApiBaseUrl, mClientId, mClientSecret);
        }
        return mUserCenterApiCommunicator;
    }

    /**
     * Helper method to create a UserCenterManager instance which is used to manage Gini user accounts.
     *
     * @return                      The UserCenterManager instance.
     */
    private synchronized UserCenterManager getUserCenterManager() {
        if (mUserCenterManager == null) {
            mUserCenterManager = new UserCenterManager(getUserCenterAPICommunicator());
        }
        return mUserCenterManager;
    }

    /**
     * Helper method to create a DocumentTaskManager instance.
     *
     * @return                      The DocumentTaskManager instance.
     */
    private synchronized DocumentTaskManager getDocumentTaskManager() {
        if (mDocumentTaskManager == null) {
            mDocumentTaskManager = new DocumentTaskManager(getApiCommunicator(), getSessionManager());
        }
        return mDocumentTaskManager;
    }

    /**
     * Return the {@link SessionManager} set via #setSessionManager. If no SessionManager has been set, default to
     * {@link AnonymousSessionManager}.
     *
     * @return                      The SessionManager instance.
     */
    public synchronized SessionManager getSessionManager() {
        if (mSessionManager == null) {
            mSessionManager = new AnonymousSessionManager(mEmailDomain, getUserCenterManager(), getCredentialsStore());
        }
        return mSessionManager;
    }
}
