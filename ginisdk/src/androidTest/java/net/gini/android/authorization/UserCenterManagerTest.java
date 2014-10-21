package net.gini.android.authorization;


import android.test.InstrumentationTestCase;

import org.json.JSONException;

import org.json.JSONObject;
import org.mockito.Mockito;

import bolts.Task;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;


public class UserCenterManagerTest extends InstrumentationTestCase {
    private UserCenterManager mUserCenterManager;
    private UserCenterAPICommunicator mMockUserCenterAPICommunicator;

    public void setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        mMockUserCenterAPICommunicator = Mockito.mock(UserCenterAPICommunicator.class);
        mUserCenterManager = new UserCenterManager(mMockUserCenterAPICommunicator);
    }

    /**
     * Many tests require a valid response from the Gini API. This method returns as JSONObject
     * which is a valid Gini API response.
     */
    public JSONObject createTestTokenResponse(final String accessToken) throws JSONException{
        return new JSONObject() {{
            put("token_type", "bearer");
            put("access_token", accessToken);
            put("expires_in", 3599);
        }};
    }

    /**
     * Creates and returns a JSONObject which is a valid Gini User Center API response for requesting a user's
     * information.
     */
    public JSONObject createLoginUserResponse(final String email) throws JSONException {
        return new JSONObject() {{
            put("id", "88a28076-18e8-4275-b39c-eaacc240d406");
            put("email", email);
        }};
    }

    public void testLoginClientReturnsFuture() {
        when(mMockUserCenterAPICommunicator.loginClient()).thenReturn(Task.forResult(new JSONObject()));

        assertNotNull(mUserCenterManager.loginClient());
    }

    public void testLoginClientResolvesToCorrectSession() throws JSONException {
        JSONObject responseData = createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6");
        when(mMockUserCenterAPICommunicator.loginClient()).thenReturn(Task.forResult(responseData));

        Session session = mUserCenterManager.loginClient().getResult();

        assertEquals("74c1e7fe-e464-451f-a6eb-8f0998c46ff6", session.getAccessToken());
    }

    public void testGetSessionReusesSession() throws JSONException {
        // First try which sets the first session.
        final JSONObject firstResponseData = createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6");
        when(mMockUserCenterAPICommunicator.loginClient()).thenReturn(Task.forResult(firstResponseData));
        Session firstSession = mUserCenterManager.getUserCenterSession().getResult();

        // Second try which should reuse the old session.
        final JSONObject secondResponseData = new JSONObject() {{
            put("token_type", "bearer");
            put("access_token", "12345678-e464-451f-a6eb-8f0998c46ff6");
            put("expires_in", 3599);
        }};
        when(mMockUserCenterAPICommunicator.loginClient()).thenReturn(Task.forResult(secondResponseData));
        Session secondSession = mUserCenterManager.getUserCenterSession().getResult();

        assertEquals(firstSession, secondSession);
    }

    public void testLoginUserShouldReturnTask() throws JSONException {
        UserCredentials userCredentials = new UserCredentials("foobar", "1234");
        JSONObject responseData = createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6");
        when(mMockUserCenterAPICommunicator.loginUser(userCredentials)).thenReturn(Task.forResult(responseData));

        assertNotNull(mUserCenterManager.loginUser(userCredentials));
    }

    public void testLoginUserShouldReturnCorrectSession() throws JSONException {
        UserCredentials userCredentials = new UserCredentials("foobar", "1234");
        JSONObject responseData = createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6");
        when(mMockUserCenterAPICommunicator.loginUser(userCredentials)).thenReturn(Task.forResult(responseData));

        Session session = mUserCenterManager.loginUser(userCredentials).getResult();
        assertNotNull(session);
        assertEquals("74c1e7fe-e464-451f-a6eb-8f0998c46ff6", session.getAccessToken());
    }

    public void testCreateUserShouldReturnTask() throws JSONException {
        when(mMockUserCenterAPICommunicator.loginClient()).thenReturn(
                Task.forResult(createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6")));
        assertNotNull(mUserCenterManager.createUser(new UserCredentials("foo", "bar")));
    }

    public void testCreateUserShouldResolveToUser() throws JSONException, InterruptedException {
        when(mMockUserCenterAPICommunicator.loginClient())
                .thenReturn(Task.forResult(createTestTokenResponse("74c1e7fe-e464-451f-a6eb-8f0998c46ff6")));
        UserCredentials userCredentials = new UserCredentials("foobar@example.com", "1234");
        when(mMockUserCenterAPICommunicator.createUser(eq(userCredentials), any(Session.class)))
                .thenReturn(Task.forResult(createLoginUserResponse("foobar@example.com")));

        Task<User> creationTask = mUserCenterManager.createUser(userCredentials);
        creationTask.waitForCompletion();
        User user = creationTask.getResult();
        assertNotNull(user);
        assertEquals("foobar@example.com", user.getUsername());
        assertEquals("88a28076-18e8-4275-b39c-eaacc240d406", user.getUserId());
    }
}
