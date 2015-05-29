package net.gini.android;

import android.test.AndroidTestCase;

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

    private class NullSessionManager implements SessionManager {

        @Override
        public Task<Session> getSession() {
            return Task.forError(new UnsupportedOperationException("NullSessionManager can't create sessions"));
        }
    }
}
