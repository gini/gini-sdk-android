package net.gini.android;


import android.net.Uri;
import android.test.InstrumentationTestCase;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;

import net.gini.android.authorization.Session;
import net.gini.android.requests.DefaultRetryPolicyFactory;
import net.gini.android.requests.RetryPolicyFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Date;
import java.util.Map;

import static com.android.volley.Request.Method.DELETE;
import static com.android.volley.Request.Method.GET;
import static com.android.volley.Request.Method.POST;
import static com.android.volley.Request.Method.PUT;
import static org.mockito.Mockito.verify;

public class ApiCommunicatorTests extends InstrumentationTestCase {
    private ApiCommunicator mApiCommunicator;
    private RequestQueue mRequestQueue;
    private RetryPolicyFactory retryPolicyFactory;

    @Override
    public void setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        retryPolicyFactory = new DefaultRetryPolicyFactory();
        mRequestQueue = Mockito.mock(RequestQueue.class);
        mApiCommunicator = new ApiCommunicator("https://api.gini.net/", mRequestQueue, retryPolicyFactory);
    }

    public byte[] createUploadData() {
        return "foobar".getBytes(Utils.CHARSET_UTF8);
    }

    public Session createSession(final String accessToken) {
        return new Session(accessToken, new Date());
    }

    public Session createSession() {
        return createSession("1234-5678-9012");
    }

    public void testConstructionThrowsNullPointerExceptionWithNullArguments() {
        try {
            new ApiCommunicator(null, null, retryPolicyFactory);
            fail("NullPointerException not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            new ApiCommunicator("https://api.gini.net", null, retryPolicyFactory);
            fail("NullPointerException not thrown");
        } catch (NullPointerException ignored) {
        }
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
            mApiCommunicator.uploadDocument(createUploadData(), MediaTypes.IMAGE_JPEG, null, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}

    }

    public void testUploadDocumentReturnsTask() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();
        assertNotNull(mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session));
    }

    public void testUploadDocumentWithNameReturnsTask() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        assertNotNull(mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, "foobar.jpg", session));
    }

    public void testUploadDocumentHasCorrectAccessToken() throws AuthFailureError {
        final byte[] documentData = createUploadData();
        final Session session = createSession("1234-5678");

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        Map headers = requestCaptor.getValue().getHeaders();
        assertEquals("BEARER 1234-5678", headers.get("Authorization"));
    }

    public void testUploadDocumentHasCorrectContentType() throws AuthFailureError {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        assertEquals(MediaTypes.IMAGE_JPEG, requestCaptor.getValue().getBodyContentType());
    }

    public void testUploadDocumentHasCorrectAcceptHeader() throws AuthFailureError {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        Map headers = requestCaptor.getValue().getHeaders();
        final String acceptHeader = (String) headers.get("Accept");
        assertTrue(acceptHeader.contains(MediaTypes.GINI_JSON_V1));
    }

    public void testUploadDocumentHasCorrectBody() throws AuthFailureError {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals(documentData, request.getBody());
    }

    public void testUploadDocumentHasCorrectUrlAndMethod() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("https://api.gini.net/documents/", request.getUrl());
        assertEquals(POST, request.getMethod());
    }

    public void testUploadDocumentSubmitsFilename() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, "foobar.jpg", null, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("https://api.gini.net/documents/?filename=foobar.jpg", request.getUrl());
    }

    public void testUploadDocumentSubmitsDoctypeHint() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, "invoice", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("https://api.gini.net/documents/?doctype=invoice", request.getUrl());
    }

    public void testDeleteDocumentsReturnsTask() {
        final Session session = createSession();

        assertNotNull(mApiCommunicator.deleteDocument("1234", session));
    }

    public void testDeleteDocumentDeletesDocument() {
        final Session session = createSession();

        mApiCommunicator.deleteDocument("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals(DELETE, request.getMethod());
    }

    public void testDeleteDocumentDeletesTheCorrectDocument() {
        final Session session = createSession();

        mApiCommunicator.deleteDocument("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://api.gini.net/documents/1234", request.getUrl());
    }

    public void testDeleteDocumentUsesTheCorrectSession() throws AuthFailureError {
        final Session session = createSession("4321-1234");

        mApiCommunicator.deleteDocument("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("Bearer 4321-1234", request.getHeaders().get("Authorization"));
    }

    public void testDeleteDocumentThrowsWithWrongArguments() {
        try {
            mApiCommunicator.deleteDocument(null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.deleteDocument("1234", null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.deleteDocument(null, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

    }

    @SuppressWarnings("ConstantConditions")
    public void testGetDocumentThrowsWithNullArguments() {
        try {
            final String documentId = null;
            mApiCommunicator.getDocument(documentId, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            final Uri documentUri = null;
            mApiCommunicator.getDocument(documentUri, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }


        try {
            mApiCommunicator.getDocument("1234", null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            final String documentId = null;
            mApiCommunicator.getDocument(documentId, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            final String documentUri = null;
            mApiCommunicator.getDocument(documentUri, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

    }

    public void testGetDocumentReturnsTask() {
        Session session = createSession();

        assertNotNull(mApiCommunicator.getDocument("1234", session));
    }

    public void testGetDocumentGetsCorrectDocument() {
        Session session = createSession();

        mApiCommunicator.getDocument("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://api.gini.net/documents/1234", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    public void testGetDocumentSendsCorrectAuthorizationHeaders() throws AuthFailureError {
        Session session = createSession("4321-1234");

        mApiCommunicator.getDocument("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 4321-1234", request.getHeaders().get("Authorization"));
    }

    public void testGetDocumentSendsCorrectAcceptHeader() throws AuthFailureError {
        Session session = createSession();

        mApiCommunicator.getDocument("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertTrue(((String) request.getHeaders().get("Accept")).contains(MediaTypes.GINI_JSON_V1));
    }

    public void testGetExtractionsThrowsWithNullArguments() {
        try {
            mApiCommunicator.getExtractions(null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.getExtractions("1234-4321", null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.getExtractions(null, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    public void testGetExtractionsGetsTheCorrectDocument() {
        Session session = createSession();

        mApiCommunicator.getExtractions("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://api.gini.net/documents/1234/extractions", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    public void testGetExtractionsHasCorrectAuthorizationHeader() throws AuthFailureError {
        Session session = createSession("1234-1234");

        mApiCommunicator.getExtractions("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 1234-1234", request.getHeaders().get("Authorization"));
    }

    public void testGetExtractionsHasCorrectAcceptHeader() throws AuthFailureError {
        Session session = createSession("1234-1234");

        mApiCommunicator.getExtractions("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertTrue(((String) request.getHeaders().get("Accept")).contains(MediaTypes.GINI_JSON_V1));
    }

    public void testGetIncubatorExtractionsThrowsWithNullArguments() {
        try {
            mApiCommunicator.getExtractions(null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.getExtractions("1234-4321", null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.getExtractions(null, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    public void testGetIncubatorExtractionsGetsTheCorrectDocument() {
        Session session = createSession();

        mApiCommunicator.getExtractions("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://api.gini.net/documents/1234/extractions", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    public void testGetIncubatorExtractionsHasCorrectAuthorizationHeader() throws AuthFailureError {
        Session session = createSession("1234-1234");

        mApiCommunicator.getIncubatorExtractions("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 1234-1234", request.getHeaders().get("Authorization"));
    }

    public void testGetIncubatorExtractionsHasCorrectAcceptHeader() throws AuthFailureError {
        Session session = createSession("1234-1234");

        mApiCommunicator.getIncubatorExtractions("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertTrue(((String) request.getHeaders().get("Accept")).contains(MediaTypes.GINI_JSON_INCUBATOR));
    }

    public void testErrorReportForDocumentsThrowsWithNullArguments() {
        try {
            mApiCommunicator.errorReportForDocument(null, null, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.errorReportForDocument("1234", null, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.errorReportForDocument(null, null, null, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.errorReportForDocument(null, "foobar", "foobar", createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    public void testErrorReportForDocumentHasCorrectUrl() {
        Session session = createSession();

        mApiCommunicator.errorReportForDocument("1234", "short summary", "and a description", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        // TODO: remove all the flakiness
        assertEquals(
                "https://api.gini.net/documents/1234/errorreport?description=and+a+description&summary=short+summary",
                request.getUrl());
    }

    public void testErrorReportForDocumentHasCorrectAuthorizationHeader() throws AuthFailureError {
        Session session = createSession("4444-2222");

        mApiCommunicator.errorReportForDocument("1234", "short summary", "and a description", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 4444-2222", request.getHeaders().get("Authorization"));
    }

    public void testErrorReportHasCorrectAcceptHeader() throws  AuthFailureError {
        Session session = createSession("4444-2222");

        mApiCommunicator.errorReportForDocument("1234", "short summary", "and a description", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertTrue(((String) request.getHeaders().get("Accept")).contains(MediaTypes.GINI_JSON_V1));
    }

    public void testSendFeedbackThrowsExceptionWithNullArguments() throws JSONException {
        try {
            mApiCommunicator.sendFeedback(null, null, null);
            fail("Exception not raised");
        } catch(NullPointerException ignored) {}

        try {
            mApiCommunicator.sendFeedback("1234-1234", new JSONObject(), null);
            fail("Exception not raised");
        } catch (NullPointerException ignored) {}

        try {
            mApiCommunicator.sendFeedback("1234-1234", null, createSession());
            fail("Exception not raised");
        } catch (NullPointerException ignored) {}

        try {
            mApiCommunicator.sendFeedback(null, new JSONObject(), createSession());
            fail("Exception not raised");
        } catch (NullPointerException ignored) {}
    }

    public void testSendFeedbackUpdatesCorrectDocument() throws JSONException {
        Session session = createSession();

        mApiCommunicator.sendFeedback("1234-1234", new JSONObject(), session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://api.gini.net/documents/1234-1234/extractions", request.getUrl());
        assertEquals(PUT, request.getMethod());
    }

    public void testSendFeedbackSendsCorrectData() throws JSONException, AuthFailureError {
        Session session = createSession();
        JSONObject extractions = new JSONObject();
        JSONObject value = new JSONObject();
        extractions.put("amountToPay", value);
        value.put("value", "32:EUR");

        mApiCommunicator.sendFeedback("1234-1234", extractions, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("{\"feedback\":{\"amountToPay\":{\"value\":\"32:EUR\"}}}", new String(request.getBody()));
    }

    public void testSendFeedbackHasCorrectAuthorizationHeader() throws AuthFailureError, JSONException {
        Session session = createSession("9999-8888-7777");

        mApiCommunicator.sendFeedback("1234", new JSONObject(), session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 9999-8888-7777", request.getHeaders().get("Authorization"));
    }

    public void testSendFeedbackHasCorrectContentType() throws AuthFailureError, JSONException {
        Session session = createSession("9999-8888-7777");

        mApiCommunicator.sendFeedback("1234", new JSONObject(), session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        final String acceptHeader = (String) request.getHeaders().get("Accept");
        assertTrue(acceptHeader.contains(MediaTypes.GINI_JSON_V1));
    }

    public void testGetPreviewThrowsWithNullArguments() {
        try {
            mApiCommunicator.getPreview(null, 0, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}

        try {
            mApiCommunicator.getPreview("1234", 1, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}

        try {
            mApiCommunicator.getPreview("1234", 1, ApiCommunicator.PreviewSize.MEDIUM, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {}
    }

    public void testGetPreviewHasCorrectUrlWithBigPreview() {
        Session session = createSession();

        mApiCommunicator.getPreview("1234", 1, ApiCommunicator.PreviewSize.BIG, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://api.gini.net/documents/1234/pages/1/1280x1810", request.getUrl());
    }

    public void testGetPreviewHasCorrectUrlWithMediumPreview() {
        Session session = createSession();

        mApiCommunicator.getPreview("1234", 1, ApiCommunicator.PreviewSize.MEDIUM, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://api.gini.net/documents/1234/pages/1/750x900", request.getUrl());
    }

    public void testGetPreviewHasCorrectAuthorizationHeader() throws AuthFailureError{
        Session session = createSession("9876-5432");

        mApiCommunicator.getPreview("1234", 1, ApiCommunicator.PreviewSize.BIG, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 9876-5432", request.getHeaders().get("Authorization"));
    }

    public void testGetPreviewHasCorrectAcceptHeader() throws AuthFailureError{
        Session session = createSession();

        mApiCommunicator.getPreview("1234", 1, ApiCommunicator.PreviewSize.MEDIUM, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals(MediaTypes.IMAGE_JPEG, request.getHeaders().get("Accept"));
    }

    public void testGetLayoutHasCorrectUrl() {
        final Session session = createSession();

        mApiCommunicator.getLayoutForDocument("1234-4321", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://api.gini.net/documents/1234-4321/layout", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    public void testGetLayoutHasCorrectAcceptHeader() throws AuthFailureError {
        final Session session = createSession();

        mApiCommunicator.getLayoutForDocument("1234-4321", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        final String acceptHeader = (String) request.getHeaders().get("Accept");
        assertTrue(acceptHeader.contains(MediaTypes.GINI_JSON_V1));
    }

    public void testGetLayoutHasCorrectAuthorizationHeader() throws AuthFailureError {
        final Session session = createSession("9999-8888-7777");

        mApiCommunicator.getLayoutForDocument("1234-4321", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 9999-8888-7777", request.getHeaders().get("Authorization"));
    }

    public void testGetDocumentListHasCorrectUrl() {
        final Session session = createSession();

        mApiCommunicator.getDocumentList(0, 23, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://api.gini.net/documents?offset=0&limit=23", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    public void testGetDocumentListHasCorrectAcceptHeader() throws AuthFailureError {
        final Session session = createSession();

        mApiCommunicator.getDocumentList(0, 20, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        final String acceptHeader = (String) request.getHeaders().get("Accept");
        assertTrue(acceptHeader.contains(MediaTypes.GINI_JSON_V1));
    }

    public void testGetDocumentListHasCorrectAuthorizationHeader() throws AuthFailureError {
        final Session session = createSession("9999-8888-7777");

        mApiCommunicator.getDocumentList(0, 20, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 9999-8888-7777", request.getHeaders().get("Authorization"));
    }

    public void testSearchDocumentsHasCorrectUrl() {
        final Session session = createSession();

        mApiCommunicator.searchDocuments("foo bär", null, 0, 20, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://api.gini.net/search?q=foo%20b%C3%A4r&offset=0&limit=20", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    public void testSearchDocumentsWithDoctypeHasCorrectUrl() {
        final Session session = createSession();

        mApiCommunicator.searchDocuments("foo bär", "invoice", 0, 20, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://api.gini.net/search?q=foo%20b%C3%A4r&offset=0&limit=20&docType=invoice", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    public void testSearchDocumentsHasCorrectAcceptHeader() throws AuthFailureError {
        final Session session = createSession();

        mApiCommunicator.searchDocuments("foobar", null, 0, 20, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        final String acceptHeader = (String) request.getHeaders().get("Accept");
        assertTrue(acceptHeader.contains(MediaTypes.GINI_JSON_V1));
    }

    public void testSearchDocumentsHasCorrectAuthorizationHeader() throws AuthFailureError {
        final Session session = createSession("9999-8888-7777");

        mApiCommunicator.searchDocuments("foobar", null, 0, 20, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 9999-8888-7777", request.getHeaders().get("Authorization"));
    }
}
