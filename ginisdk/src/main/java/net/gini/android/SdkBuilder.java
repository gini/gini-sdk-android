package net.gini.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

import net.gini.android.authorization.AnonymousSessionManager;
import net.gini.android.authorization.CredentialsStore;
import net.gini.android.authorization.PubKeyManager;
import net.gini.android.authorization.SessionManager;
import net.gini.android.authorization.SharedPreferencesCredentialsStore;
import net.gini.android.authorization.UserCenterAPICommunicator;
import net.gini.android.authorization.UserCenterManager;
import net.gini.android.requests.DefaultRetryPolicyFactory;
import net.gini.android.requests.RetryPolicyFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import static net.gini.android.Utils.checkNotNull;

public class SdkBuilder {

    private final Context mContext;

    private String mApiBaseUrl = "https://api.gini.net/";
    private String mUserCenterApiBaseUrl = "https://user.gini.net/";

    private String mEmailDomain;
    private String mClientId;
    private String mClientSecret;
    private String[] mCertificatePaths;

    private ApiCommunicator mApiCommunicator;
    private RequestQueue mRequestQueue;
    private DocumentTaskManager mDocumentTaskManager;
    private SessionManager mSessionManager;
    private CredentialsStore mCredentialsStore;
    private UserCenterManager mUserCenterManager;
    private UserCenterAPICommunicator mUserCenterApiCommunicator;
    private int mTimeoutInMs = DefaultRetryPolicy.DEFAULT_TIMEOUT_MS;
    private int mMaxRetries = DefaultRetryPolicy.DEFAULT_MAX_RETRIES;
    private float mBackOffMultiplier = DefaultRetryPolicy.DEFAULT_BACKOFF_MULT;
    private RetryPolicyFactory mRetryPolicyFactory;
    private Cache mCache;

    /**
     * Constructor to initialize a new builder instance where anonymous Gini users are used. <b>This requires access to
     * the Gini User Center API. Access to the User Center API is restricted to selected clients only.</b>
     *
     * @param context      Your application's Context instance (Android).
     * @param clientId     Your application's client ID for the Gini API.
     * @param clientSecret Your application's client secret for the Gini API.
     * @param emailDomain  The email domain which is used for created Gini users.
     */
    public SdkBuilder(final Context context, final String clientId, final String clientSecret,
                      final String emailDomain) {
        mContext = context;
        mEmailDomain = emailDomain;
        mClientSecret = clientSecret;
        mClientId = clientId;
    }

    /**
     * Constructor to initialize a new builder instance where anonymous Gini users are used. <b>This requires access to
     * the Gini User Center API. Access to the User Center API is restricted to selected clients only.</b>
     *
     * @param context                Your application's Context instance (Android).
     * @param clientId               Your application's client ID for the Gini API.
     * @param clientSecret           Your application's client secret for the Gini API.
     * @param emailDomain            The email domain which is used for created Gini users.
     * @param certificateAssetsPaths Your certificates paths relative to your assets.
     *                               (i.e: 'gini.cer' or 'certificates/gini.cer')
     */
    public SdkBuilder(final Context context, final String clientId, final String clientSecret,
                      final String emailDomain, String[] certificateAssetsPaths) {
        this(context, clientId, clientSecret, emailDomain);
        mCertificatePaths = certificateAssetsPaths;
    }

    /**
     * Constructor to initialize a new builder instance. The created Gini instance will use the given
     * {@link SessionManager} for session management.
     *
     * @param context        Your application's Context instance (Android).
     * @param sessionManager The SessionManager to use.
     */
    public SdkBuilder(final Context context, final SessionManager sessionManager) {
        mContext = context;
        mSessionManager = sessionManager;
    }

    /**
     * Set the base URL of the Gini API. Handy for tests. <b>Usually, you do not use this method</b>.
     *
     * @param newUrl The URL of the Gini API which is used by the requests of the Gini SDK.
     * @return The builder instance to enable chaining.
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
     * @param newUrl The URL of the Gini User Center API which is used by the requests of the Gini SDK.
     * @return The builder instance to enable chaining.
     */
    public SdkBuilder setUserCenterApiBaseUrl(String newUrl) {
        if (!newUrl.endsWith("/")) {
            newUrl += "/";
        }
        mUserCenterApiBaseUrl = newUrl;
        return this;
    }

    /**
     * Sets the (initial) timeout for each request. A timeout error will occur if nothing is received from the underlying socket in the given time span.
     * The initial timeout will be altered depending on the #backoffMultiplier and failed retries.
     *
     * @param connectionTimeoutInMs initial timeout
     * @return The builder instance to enable chaining.
     */
    public SdkBuilder setConnectionTimeoutInMs(final int connectionTimeoutInMs) {
        if (connectionTimeoutInMs < 0) {
            throw new IllegalArgumentException("connectionTimeoutInMs can't be less than 0");
        }
        mTimeoutInMs = connectionTimeoutInMs;
        return this;
    }

    /**
     * Sets the maximal number of retries for each network request.
     *
     * @param maxNumberOfRetries maximal number of retries.
     * @return The builder instance to enable chaining.
     */
    public SdkBuilder setMaxNumberOfRetries(final int maxNumberOfRetries) {
        if (maxNumberOfRetries < 0) {
            throw new IllegalArgumentException("maxNumberOfRetries can't be less than 0");
        }
        mMaxRetries = maxNumberOfRetries;
        return this;
    }

    /**
     * Sets the backoff multiplication factor for connection retries.
     * In case of failed retries the timeout of the last request attempt is multiplied with this factor
     *
     * @param backOffMultiplier the backoff multiplication factor
     * @return The builder instance to enable chaining.
     */
    public SdkBuilder setConnectionBackOffMultiplier(final float backOffMultiplier) {
        if (backOffMultiplier < 0.0) {
            throw new IllegalArgumentException("backOffMultiplier can't be less than 0");
        }
        mBackOffMultiplier = backOffMultiplier;
        return this;
    }

    /**
     * Set the credentials store which is used by the Gini SDK to store user credentials. If no credentials store is
     * set, the net.gini.android.authorization.SharedPreferencesCredentialsStore is used by default.
     *
     * @param credentialsStore A credentials store instance (specified by the CredentialsStore interface).
     * @return The builder instance to enable chaining.
     */
    public SdkBuilder setCredentialsStore(CredentialsStore credentialsStore) {
        mCredentialsStore = checkNotNull(credentialsStore);
        return this;
    }

    /**
     * Set the cache implementation to use with Volley. If no cache is set, the default Volley cache
     * will be used.
     *
     * @param cache A cache instance (specified by the com.android.volley.Cache interface).
     * @return The builder instance to enable chaining.
     */
    public SdkBuilder setCache(Cache cache) {
        mCache = cache;
        return this;
    }

    /**
     * Builds the Gini instance with the configuration settings of the builder instance.
     *
     * @return The fully configured Gini instance.
     */
    public Gini build() {
        return new Gini(getDocumentTaskManager(), getCredentialsStore());
    }

    /**
     * Helper method to create (and store) the RequestQueue which is used for both the requests to the Gini API and the
     * Gini User Center API.
     *
     * @return The RequestQueue instance.
     */
    private synchronized RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            if (mCache != null) {
                mRequestQueue = new RequestQueue(mCache, new BasicNetwork(createHttpStack()));
            } else {
                mRequestQueue = Volley.newRequestQueue(mContext, createHttpStack());
            }
        }
        return mRequestQueue;
    }

    /**
     * Helper method to create a HttpStack which is used to perform Http requests. Optionally it can
     * take care of certificate pinning functionality.
     *
     * @return HttpStack instance.
     */

    private synchronized HttpStack createHttpStack() {
        HttpStack stack = new HurlStack();

        if (mCertificatePaths != null && mCertificatePaths.length > 0) {
            try {
                TrustManager trustManagers[] = {new PubKeyManager(getLocalCertificatesFromAssets(mCertificatePaths))};

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagers, null);
                SSLSocketFactory pinnedSSLSocketFactory = sslContext.getSocketFactory();
                stack = new HurlStack(null, pinnedSSLSocketFactory);

            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
            }
        }

        return stack;
    }

    /**
     * Helper method to create (and store) the ApiCommunicator instance which is used to do the requests to the Gini API.
     *
     * @return The ApiCommunicator instance.
     */
    private synchronized ApiCommunicator getApiCommunicator() {
        if (mApiCommunicator == null) {
            mApiCommunicator = new ApiCommunicator(mApiBaseUrl, getRequestQueue(), getRetryPolicyFactory());
        }
        return mApiCommunicator;
    }

    /**
     * Helper method to create (and store) the instance of the CredentialsStore implementation which is used to store
     * user credentials. If the credentials store was previously configured via the builder, the previously configured
     * instance is used. Otherwise, a net.gini.android.authorization.SharedPreferencesCredentialsStore instance is
     * created by default.
     *
     * @return The CredentialsStore instance.
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
     * @return The ApiCommunicator instance.
     */
    private synchronized UserCenterAPICommunicator getUserCenterAPICommunicator() {
        if (mUserCenterApiCommunicator == null) {
            mUserCenterApiCommunicator =
                    new UserCenterAPICommunicator(getRequestQueue(), mUserCenterApiBaseUrl, mClientId, mClientSecret,
                            getRetryPolicyFactory());
        }
        return mUserCenterApiCommunicator;
    }

    /**
     * Helper method to create a {@link RetryPolicyFactory} instance which is used to create a
     * {@link com.android.volley.RetryPolicy} for each request.
     *
     * @return The RetryPolicyFactory instance.
     */
    private synchronized RetryPolicyFactory getRetryPolicyFactory() {
        if (mRetryPolicyFactory == null) {
            mRetryPolicyFactory = new DefaultRetryPolicyFactory(mTimeoutInMs, mMaxRetries, mBackOffMultiplier);
        }
        return mRetryPolicyFactory;
    }

    /**
     * Helper method to create a UserCenterManager instance which is used to manage Gini user accounts.
     *
     * @return The UserCenterManager instance.
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
     * @return The DocumentTaskManager instance.
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
     * @return The SessionManager instance.
     */
    public synchronized SessionManager getSessionManager() {
        if (mSessionManager == null) {
            mSessionManager = new AnonymousSessionManager(mEmailDomain, getUserCenterManager(), getCredentialsStore());
        }
        return mSessionManager;
    }

    /**
     * Helper method to get local certificates from assets
     *
     * @param certFilePaths An array containing all certificates paths relatively to the assets
     * @return Local certificates
     * @throws IllegalArgumentException if the the certificate is not found or it is invalid.
     */

    private synchronized X509Certificate[] getLocalCertificatesFromAssets(String[] certFilePaths) {
        List<X509Certificate> certificates = new ArrayList<>();
        AssetManager assetManager = mContext.getAssets();
        try {
            for (String fileName : certFilePaths) {
                InputStream fis = assetManager.open(fileName);
                X509Certificate certificate = createCertificate(fis);
                if (certificate != null) {
                    certificates.add(certificate);
                }
                fis.close();
            }
        } catch (IOException | CertificateException e) {
            throw new IllegalArgumentException("It is not a valid certificate or it does not exist in the assets: ", e.getCause());
        }
        return certificates.toArray(new X509Certificate[certificates.size()]);
    }

    /**
     * Helper method to create certificate from and InputStream
     *
     * @param inputStream Certificate generated with the input stream
     * @return Generated cetificate
     * @throws IOException          if the the certificate is not found or it is invalid.
     * @throws CertificateException if parsing problems are detected when generating Certificate
     */

    private synchronized X509Certificate createCertificate(InputStream inputStream) throws IOException, CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        BufferedInputStream bis = new BufferedInputStream(inputStream);

        if (bis.available() > 0) {
            return (X509Certificate) cf.generateCertificate(bis);
        } else {
            return null;
        }
    }
}

