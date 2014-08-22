package net.gini.android;


import android.test.AndroidTestCase;

import net.gini.android.authorization.Session;


public class SessionTest extends AndroidTestCase {
    public void testGetSessionSetter() {
        Session session = new Session("1234-5678-9101");
        assertEquals(session.getAccessToken(), "1234-5678-9101");
    }
}
