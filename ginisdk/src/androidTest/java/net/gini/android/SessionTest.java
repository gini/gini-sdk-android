package net.gini.android;


import android.support.test.filters.SmallTest;
import android.test.AndroidTestCase;

import net.gini.android.authorization.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

@SmallTest
public class SessionTest extends AndroidTestCase {
    /** Helper method which creates a valid JSON object that can be used in tests. */
    public JSONObject createTestResponse() throws JSONException{
        return new JSONObject(){{
            put("token_type", "bearer");
            put("access_token", "74c1e7fe-e464-451f-a6eb-8f0998c46ff6");
            put("expires_in", 3599);
        }};
    }

    public void testGetSessionSetter() {
        Session session = new Session("1234-5678-9101", new Date());
        assertEquals("1234-5678-9101", session.getAccessToken());
    }

    public void testFactoryReturnsSession() throws JSONException {
        JSONObject responseData = createTestResponse();

        assertNotNull(Session.fromAPIResponse(responseData));
    }

    public void testFactorySetsCorrectAccessToken() throws JSONException {
        JSONObject responseData = createTestResponse();

        Session session = Session.fromAPIResponse(responseData);

        assertEquals("74c1e7fe-e464-451f-a6eb-8f0998c46ff6", session.getAccessToken());
    }

    public void testFactorySetsCorrectExpirationDate() throws JSONException {
        JSONObject responseData = createTestResponse();

        Session session = Session.fromAPIResponse(responseData);

        assertFalse(session.hasExpired());
    }
}
