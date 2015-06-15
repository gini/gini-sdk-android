package net.gini.android;

import android.test.AndroidTestCase;

import com.android.volley.RetryPolicy;

import net.gini.android.authorization.Session;
import net.gini.android.authorization.SessionManager;

import bolts.Task;

import static org.mockito.Mockito.verify;

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
            builder.setConnectionMaxNumberOfRetries(-1);
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
        builder.setConnectionMaxNumberOfRetries(66);
        builder.setConnectionBackOffMultiplier(1.3636f);
        Gini gini = builder.build();

        RetryPolicy retryPolicy = gini.getDocumentTaskManager().mApiCommunicator.mRetryPolicy;
        assertEquals(3333, retryPolicy.getCurrentTimeout());
    }

    private class NullSessionManager implements SessionManager {

        @Override
        public Task<Session> getSession() {
            return Task.forError(new UnsupportedOperationException("NullSessionManager can't create sessions"));
        }
    }
}
