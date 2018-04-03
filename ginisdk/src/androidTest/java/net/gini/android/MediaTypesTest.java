package net.gini.android;

import android.test.AndroidTestCase;


public class MediaTypesTest extends AndroidTestCase {
    public void testGINI_JSON_V1() {
        assertEquals("application/vnd.gini.v1+json", MediaTypes.GINI_JSON_V1);
    }

    public void testGINI_JSON_V2() {
        assertEquals("application/vnd.gini.v2+json", MediaTypes.GINI_JSON_V2);
    }

    public void testGINI_JSON_INCUBATOR() {
        assertEquals("application/vnd.gini.incubator+json", MediaTypes.GINI_JSON_INCUBATOR);
    }

    public void testAPPLICATION_JSON() {
        assertEquals("application/json", MediaTypes.APPLICATION_JSON);
    }

    public void testAPPLICATION_FORM_URLENCODED() {
        assertEquals("application/x-www-form-urlencoded", MediaTypes.APPLICATION_FORM_URLENCODED);
    }

    public void testIMAGE_JPEG() {
        assertEquals("image/jpeg", MediaTypes.IMAGE_JPEG);
    }
}
