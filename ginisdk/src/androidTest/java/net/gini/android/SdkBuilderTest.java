package net.gini.android;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.Cache;
import com.android.volley.RetryPolicy;

import net.gini.android.authorization.Session;
import net.gini.android.authorization.SessionManager;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import bolts.Task;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SdkBuilderTest {

    @Test
    public void testBuilderReturnsSdkInstance() {
        SdkBuilder builder = new SdkBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        assertNotNull(builder.build());
    }

    @Test
    public void testBuilderReturnsCorrectConfiguredSdkInstance() {
        SdkBuilder builder = new SdkBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        Gini sdkInstance = builder.build();

        assertNotNull(sdkInstance.getDocumentTaskManager());
        assertNotNull(sdkInstance.getCredentialsStore());
    }

    @Test
    public void testBuilderWorksWithAlternativeSessionManager() {
        final SessionManager sessionManager = new NullSessionManager();

        final SdkBuilder builder = new SdkBuilder(getApplicationContext(), sessionManager);
        final Gini sdkInstance = builder.build();

        assertNotNull(sdkInstance);
        assertNotNull(sdkInstance.getDocumentTaskManager());
        assertNotNull(sdkInstance.getCredentialsStore());
    }

    @Test
    public void testSetWrongConnectionTimeout() {
        SdkBuilder builder = new SdkBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setConnectionTimeoutInMs(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testSetWrongConnectionMaxNumberOfRetries() {
        SdkBuilder builder = new SdkBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setMaxNumberOfRetries(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testSetWrongConnectionBackOffMultiplier() {
        SdkBuilder builder = new SdkBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setConnectionBackOffMultiplier(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc) {
        }
    }

    @Test
    public void testRetryPolicyWiring() {
        SdkBuilder builder = new SdkBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        builder.setConnectionTimeoutInMs(3333);
        builder.setMaxNumberOfRetries(66);
        builder.setConnectionBackOffMultiplier(1.3636f);
        Gini gini = builder.build();

        final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();
        final RetryPolicy retryPolicy = documentTaskManager.mApiCommunicator.mRetryPolicyFactory.newRetryPolicy();
        assertEquals(3333, retryPolicy.getCurrentTimeout());
        assertEquals(0, retryPolicy.getCurrentRetryCount());
    }

    @Test
    public void testVolleyCacheConfiguration() {
        SdkBuilder builder = new SdkBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        NullCache nullCache = new NullCache();
        builder.setCache(nullCache);
        Gini sdkInstance = builder.build();

        assertSame(sdkInstance.getDocumentTaskManager().mApiCommunicator.mRequestQueue.getCache(), nullCache);
    }

    @Test
    public void allowsSettingCustomTrustManager() {
        SdkBuilder builder = new SdkBuilder(getApplicationContext(), "clientId", "clientSecret", "@example.com");
        final TrustManager trustManager = new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        Gini sdkInstance = builder
                .setTrustManager(trustManager)
                .build();

        assertNotNull(sdkInstance);
    }

    private static final class NullCache implements Cache {

        @Override
        public Entry get(final String key) {
            return null;
        }

        @Override
        public void put(final String key, final Entry entry) {
        }

        @Override
        public void initialize() {
        }

        @Override
        public void invalidate(final String key, final boolean fullExpire) {
        }

        @Override
        public void remove(final String key) {
        }

        @Override
        public void clear() {
        }
    }

    private class NullSessionManager implements SessionManager {

        @Override
        public Task<Session> getSession() {
            return Task.forError(new UnsupportedOperationException("NullSessionManager can't create sessions"));
        }
    }
}
