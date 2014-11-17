package net.gini.android;

import android.test.AndroidTestCase;

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
}
