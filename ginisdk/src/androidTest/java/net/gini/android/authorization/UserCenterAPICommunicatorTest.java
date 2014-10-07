package net.gini.android.authorization;

import android.os.SystemClock;
import android.test.AndroidTestCase;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;

import net.gini.android.helpers.MockNetwork;


public class UserCenterAPICommunicatorTest extends AndroidTestCase {
    private UserCenterAPICommunicator apiManager;
    private MockNetwork network;

    @Override
    public void setUp() {
        network = new MockNetwork();
        RequestQueue requestQueue = new RequestQueue(new NoCache(), network);
        requestQueue.start();
        apiManager = new UserCenterAPICommunicator(requestQueue, "https://user.gini.net/", "foobar", "1234");
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
        waitForRequests();

        assertEquals("https://user.gini.net/oauth/token?grant_type=client_credentials", network.mLastRequest.getUrl());
    }


    public void testLoginUserShouldReturnTask() {
        assertNotNull(apiManager.loginClient());
    }

    public void testLoginUserRequestHasCorrectUrl() {
        UserCredentials userCredentials = new UserCredentials("foobar", "1234");
        apiManager.loginUser(userCredentials);
        waitForRequests();

        assertEquals("https://user.gini.net/oauth/token?grant_type=password", network.mLastRequest.getUrl());
    }

    public void testLoginUserRequestHasCorrectData() throws AuthFailureError {
        UserCredentials userCredentials = new UserCredentials("foobar", "1234");
        apiManager.loginUser(userCredentials);
        waitForRequests();

        assertEquals("password=1234&username=foobar", new String(network.mLastRequest.getBody()));
    }
}
