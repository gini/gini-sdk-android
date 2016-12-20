package net.gini.android.authorization;

import static net.gini.android.helpers.TestUtils.areEqualURIQueries;

import static org.mockito.Mockito.verify;

import android.os.SystemClock;
import android.test.InstrumentationTestCase;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;

import net.gini.android.requests.DefaultRetryPolicyFactory;
import net.gini.android.requests.RetryPolicyFactory;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;


public class UserCenterAPICommunicatorTest extends InstrumentationTestCase {
    private UserCenterAPICommunicator apiManager;
    private RequestQueue mRequestQueue;
    private RetryPolicyFactory retryPolicyFactory;

    @Override
    public void setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        retryPolicyFactory = new DefaultRetryPolicyFactory();
        mRequestQueue = Mockito.mock(RequestQueue.class);
        apiManager = new UserCenterAPICommunicator(mRequestQueue, "https://user.gini.net/", "foobar", "1234",
                                                   retryPolicyFactory);
    }

    /**
     * Requests are executed by volley on another thread (NetworkDispatcher). This is a really dump
     * helper method which blocks the current thread so the network thread will finish and you can
     * make assertions on the network instance. Meant to be replaced by a more clever solution.
     */
    private void waitForRequests() {
        SystemClock.sleep(2);
    }

    public void testLoginClientShouldReturnTask() {
        assertNotNull(apiManager.loginClient());
    }

    public void testLoginClientRequestHasCorrectUrl() {
        apiManager.loginClient();

        final ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://user.gini.net/oauth/token?grant_type=client_credentials",
                     request.getUrl());
    }


    public void testLoginUserShouldReturnTask() {
        assertNotNull(apiManager.loginClient());
    }

    public void testLoginUserRequestHasCorrectUrl() {
        UserCredentials userCredentials = new UserCredentials("foobar", "1234");
        apiManager.loginUser(userCredentials);

        final ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://user.gini.net/oauth/token?grant_type=password", request.getUrl());
    }

    public void testLoginClientHasCorrectData() throws AuthFailureError {
        apiManager.loginClient();

        final ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("Basic Zm9vYmFyOjEyMzQ=", request.getHeaders().get("Authorization"));
    }

    public void testLoginUserRequestHasCorrectData() throws AuthFailureError {
        UserCredentials userCredentials = new UserCredentials("foobar", "1234");
        apiManager.loginUser(userCredentials);
        waitForRequests();

        final ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertTrue(areEqualURIQueries("username=foobar&password=1234", new String(request.getBody())));
    }
}
