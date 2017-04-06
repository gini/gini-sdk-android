package net.gini.android.authorization;

import android.test.InstrumentationTestCase;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import bolts.Task;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class AnonymousSessionManagerTests extends InstrumentationTestCase {
    private AnonymousSessionManager mAnonymousSessionSessionManager;
    private UserCenterManager mUserCenterManager;
    private CredentialsStore mCredentialsStore;

    @Override
    public void setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        mUserCenterManager = Mockito.mock(UserCenterManager.class);
        mCredentialsStore = Mockito.mock(CredentialsStore.class);
        mAnonymousSessionSessionManager = new AnonymousSessionManager("gini.net", mUserCenterManager, mCredentialsStore);
    }

    public void testConstructionWithNullReferencesThrowsNullPointerException() {
        try {
            new AnonymousSessionManager(null, null, null);
            fail("NullPointerException not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            new AnonymousSessionManager("foobar", null, null);
            fail("NullPointerException not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            new AnonymousSessionManager("foobar", mUserCenterManager, null);
            fail("NullPointerException not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    public void testGetSessionShouldReturnTask() {
        // Task that will never complete. Sufficient for this test.
        Task<User>.TaskCompletionSource completionSource = Task.create();
        when(mUserCenterManager.createUser(any(UserCredentials.class))).thenReturn(completionSource.getTask());

        assertNotNull(mAnonymousSessionSessionManager.getSession());
    }

    public void testGetSessionShouldResolveToSession() throws InterruptedException {
        UserCredentials userCredentials = new UserCredentials("foobar@example.com", "1234");
        when(mCredentialsStore.getUserCredentials()).thenReturn(userCredentials);
        when(mUserCenterManager.loginUser(userCredentials))
                .thenReturn(Task.forResult(new Session(UUID.randomUUID().toString(), new Date())));

        Task<Session> sessionTask = mAnonymousSessionSessionManager.getSession();
        sessionTask.waitForCompletion();

        assertNotNull(sessionTask.getResult());
    }

    public void testLoginUserShouldReturnTask() {
        // Task that will never complete. Sufficient for this test.
        Task<User>.TaskCompletionSource completionSource = Task.create();
        when(mUserCenterManager.createUser(any(UserCredentials.class))).thenReturn(completionSource.getTask());

        assertNotNull(mAnonymousSessionSessionManager.loginUser());
    }

    public void testLoginUserShouldResolveToSession() throws InterruptedException {
        UserCredentials userCredentials = new UserCredentials("foobar@example.com", "1234");
        when(mCredentialsStore.getUserCredentials()).thenReturn(userCredentials);
        when(mUserCenterManager.loginUser(userCredentials))
                .thenReturn(Task.forResult(new Session(UUID.randomUUID().toString(), new Date())));

        Task<Session> loginTask = mAnonymousSessionSessionManager.loginUser();
        loginTask.waitForCompletion();

        assertNotNull(loginTask.getResult());
    }


    public void testThatNewUserCredentialsAreStored() throws InterruptedException {
        // TODO: The returned "created" user has another email address than the UserCredentials instance which is given
        //       to the mock.
        User fakeUser = new User("1234-5678-9012-3456", "foobar@example.com");
        when(mUserCenterManager.createUser(any(UserCredentials.class))).thenReturn(Task.forResult(fakeUser));

        Task<Session> sessionTask = mAnonymousSessionSessionManager.getSession();
        sessionTask.waitForCompletion();

        verify(mCredentialsStore).storeUserCredentials(any(UserCredentials.class));
    }

    public void testThatStoredUserCredentialsAreUsed() throws InterruptedException {
        UserCredentials userCredentials = new UserCredentials("foo@example.com", "1234");
        when(mCredentialsStore.getUserCredentials()).thenReturn(userCredentials);

        Session session = new Session("1234-5678-9012", new Date());
        when(mUserCenterManager.loginUser(userCredentials)).thenReturn(Task.forResult(session));

        Task<Session> sessionTask = mAnonymousSessionSessionManager.getSession();
        sessionTask.waitForCompletion();
        assertSame(session, sessionTask.getResult());
    }

    @SuppressWarnings("unchecked")
    public void testThatUserSessionsAreReused() throws InterruptedException {
        when(mCredentialsStore.getUserCredentials()).thenReturn(new UserCredentials("foo@example.com", "1234"));
        when(mUserCenterManager.loginUser(any(UserCredentials.class))).thenReturn(
                Task.forResult(new Session(UUID.randomUUID().toString(), new Date(new Date().getTime() + 10000))),
                Task.forResult(new Session(UUID.randomUUID().toString(), new Date()))
        );

        Task<Session> firstSessionTask = mAnonymousSessionSessionManager.getSession();
        firstSessionTask.waitForCompletion();

        Task<Session> secondSessionTask = mAnonymousSessionSessionManager.getSession();
        secondSessionTask.waitForCompletion();

        assertSame(firstSessionTask.getResult(), secondSessionTask.getResult());
    }

    @SuppressWarnings("unchecked")
    public void testThatUserSessionsAreNotReusedWhenTimedOut() throws InterruptedException {
        when(mCredentialsStore.getUserCredentials()).thenReturn(new UserCredentials("foo@example.com", "1234"));
        when(mUserCenterManager.loginUser(any(UserCredentials.class))).thenReturn(
                Task.forResult(new Session(UUID.randomUUID().toString(), new Date(new Date().getTime() - 10000))),
                Task.forResult(new Session(UUID.randomUUID().toString(), new Date()))
        );


        Task<Session> firstSessionTask = mAnonymousSessionSessionManager.getSession();
        firstSessionTask.waitForCompletion();

        assertTrue(firstSessionTask.getResult().hasExpired());

        Task<Session> secondSessionTask = mAnonymousSessionSessionManager.getSession();
        secondSessionTask.waitForCompletion();

        assertNotSame(firstSessionTask.getResult(), secondSessionTask.getResult());
    }

    public void testThatCreatedUserNamesAreEmailAddresses() throws InterruptedException {
        // TODO: The returned "created" user has another email address than the UserCredentials instance which is given
        //       to the mock.
        User fakeUser = new User("1234-5678-9012-3456", "foobar@example.com");
        when(mUserCenterManager.createUser(any(UserCredentials.class))).thenReturn(Task.forResult(fakeUser));

        Task<Session> sessionTask = mAnonymousSessionSessionManager.getSession();
        sessionTask.waitForCompletion();

        ArgumentCaptor<UserCredentials> userCredentialsCaptor = ArgumentCaptor.forClass(UserCredentials.class);
        verify(mUserCenterManager).createUser(userCredentialsCaptor.capture());

        assertTrue(userCredentialsCaptor.getValue().getUsername().endsWith("@gini.net"));
    }

    @SuppressWarnings("unchecked")
    public void testThatExistingUserIsDeletedAndNewUserIsCreatedIfExistingIsInvalid() throws InterruptedException {
        when(mCredentialsStore.getUserCredentials()).thenReturn(new UserCredentials("foo@example.com", "1234"));
        when(mUserCenterManager.loginUser(any(UserCredentials.class)))
                .thenReturn(Task.<Session>forError(new VolleyError(new NetworkResponse(400, null, Collections.<String, String>emptyMap(), true))))
                .thenReturn(Task.forResult(new Session(UUID.randomUUID().toString(), new Date())));

        Task<Session> sessionTask = mAnonymousSessionSessionManager.getSession();
        sessionTask.waitForCompletion();

        verify(mCredentialsStore).deleteUserCredentials();
        verify(mUserCenterManager).createUser(any(UserCredentials.class));
    }
}
