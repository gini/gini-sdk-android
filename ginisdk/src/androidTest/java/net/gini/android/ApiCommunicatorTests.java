package net.gini.android;


import android.test.InstrumentationTestCase;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;

import net.gini.android.authorization.Session;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Date;
import java.util.Map;

import static com.android.volley.Request.Method.POST;
import static org.mockito.Mockito.verify;

public class ApiCommunicatorTests extends InstrumentationTestCase {
    private ApiCommunicator mApiCommunicator;
    private RequestQueue mRequestQueue;

    @Override
    public void setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        mRequestQueue = Mockito.mock(RequestQueue.class);
        mApiCommunicator = new ApiCommunicator("https://api.gini.net/", mRequestQueue);
    }

    public byte[] createUploadData() {
        return "foobar".getBytes();
    }

    public Session createSession(final String accessToken) {
        return new Session(accessToken, new Date());
    }

    public Session createSession() {
        return createSession("1234-5678-9012");
    }

    public void testConstructionThrowsNullPointerExceptionWithNullArguments() {
        try {
            new ApiCommunicator(null, null);
            fail("NullPointerException not thrown");
        } catch (NullPointerException ignored) {}

        try {
            new ApiCommunicator("https://api.gini.net", null);
            fail("NullPointerException not thrown");
        } catch (NullPointerException ignored) {}
    }

    public void testUploadDocumentThrowsWithNullArguments() {
        try {
            mApiCommunicator.uploadDocument(null, null, null, null, null);
            fail("Exception not thrown");
        } catch(NullPointerException ignored) {}

        try {
            mApiCommunicator.uploadDocument(null, "image/jpeg", null, null, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}

        try {
            mApiCommunicator.uploadDocument(createUploadData(), null, null, null, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}

        try {
            mApiCommunicator.uploadDocument(createUploadData(), "image/jpeg", null, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}

    }

    public void testUploadDocumentReturnsTask() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();
        assertNotNull(mApiCommunicator.uploadDocument(documentData, "image/jpeg", null, null, session));
    }

    public void testUploadDocumentWithNameReturnsTask() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        assertNotNull(mApiCommunicator.uploadDocument(documentData, "image/jpeg", null, "foobar.jpg", session));
    }

    public void testUploadDocumentHasCorrectAccessToken() throws AuthFailureError {
        final byte[] documentData = createUploadData();
        final Session session = createSession("1234-5678");

        mApiCommunicator.uploadDocument(documentData, "image/jpeg", null, null, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        Map headers = requestCaptor.getValue().getHeaders();
        assertEquals("Bearer 1234-5678", headers.get("Authorization"));
    }

    public void testUploadDocumentHasCorrectContentType() throws AuthFailureError {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, "image/jpeg", null, null, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        Map headers = requestCaptor.getValue().getHeaders();
        assertEquals("image/jpeg", headers.get("Content-Type"));
    }

    public void testUploadDocumentHasCorrectAcceptHeader() throws AuthFailureError {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, "image/jpeg", null, null, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        Map headers = requestCaptor.getValue().getHeaders();
        final String acceptHeader = (String) headers.get("Accept");
        assertTrue(acceptHeader.contains("application/vnd.gini.v1+json"));
    }

    public void testUploadDocumentHasCorrectBody() throws AuthFailureError {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, "image/jpeg", null, null, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals(documentData, request.getBody());
    }

    public void testUploadDocumentHasCorrectUrlAndMethod() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, "image/jpeg", null, null, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("https://api.gini.net/documents/?", request.getUrl());
        assertEquals(POST, request.getMethod());
    }

    public void testUploadDocumentSubmitsFilename() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, "image/jpeg", "foobar.jpg", null, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("https://api.gini.net/documents/?filename=foobar.jpg", request.getUrl());
    }

    public void testUploadDocumentSubmitsDoctypeHint() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, "image/jpeg", null, "invoice", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("https://api.gini.net/documents/?doctype=invoice", request.getUrl());
    }
}
