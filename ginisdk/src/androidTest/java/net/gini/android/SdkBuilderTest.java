package net.gini.android;

import android.test.AndroidTestCase;

import com.android.volley.Cache;
import com.android.volley.RetryPolicy;

import net.gini.android.authorization.Session;
import net.gini.android.authorization.SessionManager;

import bolts.Task;

public class SdkBuilderTest extends AndroidTestCase {

    public void testBuilderReturnsSdkInstance() {
        SdkBuilder builder = new SdkBuilder(getContext(), "clientId", "clientSecret", "@example.com");
        assertNotNull(builder.build());
    }

    public void testBuilderReturnsCorrectConfiguredSdkInstance() {
        SdkBuilder builder = new SdkBuilder(getContext(), "clientId", "clientSecret", "@example.com");
        Gini sdkInstance = builder.build();

        assertNotNull(sdkInstance.getDocumentTaskManager());
        assertNotNull(sdkInstance.getCredentialsStore());
    }

    public void testBuilderWorksWithAlternativeSessionManager() {
        final SessionManager sessionManager = new NullSessionManager();

        final SdkBuilder builder = new SdkBuilder(getContext(), sessionManager);
        final Gini sdkInstance = builder.build();

        assertNotNull(sdkInstance);
        assertNotNull(sdkInstance.getDocumentTaskManager());
        assertNotNull(sdkInstance.getCredentialsStore());
     }

    public void testSetWrongConnectionTimeout(){
        SdkBuilder builder = new SdkBuilder(getContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setConnectionTimeoutInMs(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc){}
    }

    public void testSetWrongConnectionMaxNumberOfRetries(){
        SdkBuilder builder = new SdkBuilder(getContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setMaxNumberOfRetries(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc){}
    }

    public void testSetWrongConnectionBackOffMultiplier(){
        SdkBuilder builder = new SdkBuilder(getContext(), "clientId", "clientSecret", "@example.com");
        try {
            builder.setConnectionBackOffMultiplier(-1);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException exc){}
    }

    public void testRetryPolicyWiring(){
        SdkBuilder builder = new SdkBuilder(getContext(), "clientId", "clientSecret", "@example.com");
        builder.setConnectionTimeoutInMs(3333);
        builder.setMaxNumberOfRetries(66);
        builder.setConnectionBackOffMultiplier(1.3636f);
        Gini gini = builder.build();

        final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();
        final RetryPolicy retryPolicy = documentTaskManager.mApiCommunicator.mRetryPolicyFactory.newRetryPolicy();
        assertEquals(3333, retryPolicy.getCurrentTimeout());
        assertEquals(0, retryPolicy.getCurrentRetryCount());
    }

    public void testVolleyCacheConfiguration() {
        SdkBuilder builder = new SdkBuilder(getContext(), "clientId", "clientSecret", "@example.com");
        NullCache nullCache = new NullCache();
        builder.setCache(nullCache);
        Gini sdkInstance = builder.build();

        assertSame(sdkInstance.getDocumentTaskManager().mApiCommunicator.mRequestQueue.getCache(), nullCache);
    }

    private static final class NullCache implements Cache {

        @Override
        public Entry get(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(final String key, final Entry entry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void initialize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void invalidate(final String key, final boolean fullExpire) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    private class NullSessionManager implements SessionManager {

        @Override
        public Task<Session> getSession() {
            return Task.forError(new UnsupportedOperationException("NullSessionManager can't create sessions"));
        }
    }
}
