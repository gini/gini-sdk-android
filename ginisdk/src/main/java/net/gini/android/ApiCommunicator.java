package net.gini.android;

import android.graphics.Bitmap;
import android.net.Uri;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;

import net.gini.android.authorization.Session;
import net.gini.android.authorization.requests.BearerJsonObjectRequest;
import net.gini.android.requests.BearerUploadRequest;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import bolts.Task;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.android.volley.Request.Method.DELETE;
import static com.android.volley.Request.Method.GET;
import static com.android.volley.Request.Method.POST;
import static com.android.volley.Request.Method.PUT;
import static net.gini.android.Utils.checkNotNull;
import static net.gini.android.Utils.mapToUrlEncodedString;


/**
 * The ApiCommunicator is responsible for communication with the Gini API. It only converts the server's responses to
 * more convenient objects (e.g. a JSON response to a JSONObject) but does not interpret the results in any way.
 * Therefore it is not recommended to use the ApiCommunicator directly, but to use the DocumentTaskManager instead which
 * provides much more convenient methods to work with the Gini API and uses defined models.
 */
public class ApiCommunicator {

    private final Uri mBaseUri;
    private final RequestQueue mRequestQueue;


    public ApiCommunicator(final String baseUriString, final RequestQueue mRequestQueue) {
        mBaseUri = Uri.parse(checkNotNull(baseUriString));
        this.mRequestQueue = checkNotNull(mRequestQueue);
    }

    public Task<Uri> uploadDocument(final byte[] documentData, final String contentType,
                                    @Nullable final String documentName, @Nullable final String docTypeHint,
                                    final Session session) {

        final HashMap<String, String> requestQueryData = new HashMap<String, String>();
        if (documentName != null) {
            requestQueryData.put("filename", documentName);
        }
        if (docTypeHint != null) {
            requestQueryData.put("doctype", docTypeHint);
        }
        final String url = mBaseUri.buildUpon().path("documents/").encodedQuery(mapToUrlEncodedString(requestQueryData))
                .toString();
        final RequestTaskCompletionSource<Uri> completionSource = RequestTaskCompletionSource.newCompletionSource();
        final BearerUploadRequest request =
                new BearerUploadRequest(POST, url, checkNotNull(documentData), checkNotNull(contentType), session,
                        completionSource, completionSource);
        mRequestQueue.add(request);

        return completionSource.getTask();
    }

    public Task<JSONObject> getDocument(final String documentId, final Session session) {
        final String url = mBaseUri.buildUpon().path("documents/" + checkNotNull(documentId)).toString();
        return getDocument(Uri.parse(url), session);
    }

    public Task<JSONObject> getDocument(final Uri documentUri, final Session session) {
        final String url = uriRelativeToBaseUri(documentUri).toString();
        return doRequestWithJsonResponse(url, GET, session);
    }

    public Task<JSONObject> getExtractions(final String documentId, final Session session) {
        final String url = mBaseUri.buildUpon().path(String.format("documents/%s/extractions",
                                                                   checkNotNull(documentId))).toString();
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final BearerJsonObjectRequest request =
                new BearerJsonObjectRequest(GET, url, null, checkNotNull(session), completionSource, completionSource);
        mRequestQueue.add(request);

        return completionSource.getTask();
    }

    public Task<JSONObject> getIncubatorExtractions(final String documentId, final Session session) {
        final String url = mBaseUri.buildUpon().path(String.format("documents/%s/extractions",
                checkNotNull(documentId))).toString();
        final RequestTaskCompletionSource<JSONObject> completionSource = RequestTaskCompletionSource
                .newCompletionSource();
        final BearerJsonObjectRequest request = new BearerJsonObjectRequest(GET, url, null, checkNotNull(session),
                completionSource, completionSource) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = super.getHeaders();
                // The incubator is discriminated from the "normal" extractions by the accept header.
                headers.put("Accept", MediaTypes.GINI_JSON_INCUBATOR);
                return headers;
            }
        };
        mRequestQueue.add(request);

        return completionSource.getTask();
    }

    public Task<String> deleteDocument(final String documentId, final Session session) {
        final String accessToken = checkNotNull(session).getAccessToken();
        final String url = mBaseUri.buildUpon().path("documents/" + checkNotNull(documentId)).toString();
        final RequestTaskCompletionSource<String> completionSource = RequestTaskCompletionSource.newCompletionSource();
        final StringRequest request = new StringRequest(DELETE, url, completionSource, completionSource) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        mRequestQueue.add(request);

        return completionSource.getTask();
    }

    public Task<JSONObject> errorReportForDocument(final String documentId, @Nullable final String summary,
                                                   @Nullable final String description, final Session session) {
        final HashMap<String, String> requestParams = new HashMap<String, String>();
        requestParams.put("summary", summary);
        requestParams.put("description", description);
        final String url = mBaseUri.buildUpon().path("documents/" + checkNotNull(documentId) + "/errorreport")
                .encodedQuery(mapToUrlEncodedString(requestParams)).toString();
        return doRequestWithJsonResponse(url, POST, session);
    }

    public Task<JSONObject> sendFeedback(final String documentId, final JSONObject extractions, final Session session)
            throws JSONException {
        final String url = mBaseUri.buildUpon().path(String.format("documents/%s/extractions",
                checkNotNull(documentId))).toString();
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final JSONObject requestData = new JSONObject();
        requestData.put("feedback", checkNotNull(extractions));
        final BearerJsonObjectRequest request =
                new BearerJsonObjectRequest(PUT, url, requestData, checkNotNull(session),
                        completionSource, completionSource);
        mRequestQueue.add(request);

        return completionSource.getTask();
    }

    public Task<Bitmap> getPreview(final String documentId, final int pageNumber,
                                   PreviewSize previewSize, final Session session) {
        final String url = mBaseUri.buildUpon().path(String.format("documents/%s/pages/%s/%s",
                checkNotNull(documentId), pageNumber,
                previewSize.getDimensions())).toString();
        final String accessToken = checkNotNull(session).getAccessToken();
        RequestTaskCompletionSource<Bitmap> completionSource = RequestTaskCompletionSource.newCompletionSource();
        final ImageRequest imageRequest = new ImageRequest(url, completionSource, 0, 0, ARGB_8888, completionSource) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "BEARER " + accessToken);
                headers.put("Accept", MediaTypes.IMAGE_JPEG);
                return headers;
            }
        };
        mRequestQueue.add(imageRequest);

        return completionSource.getTask();
    }

    public Task<JSONObject> getLayoutForDocument(final String documentId, final Session session) {
        final String url =
                mBaseUri.buildUpon().path(String.format("/documents/%s/layout", checkNotNull(documentId))).toString();
        return doRequestWithJsonResponse(url, GET, session);
    }

    public Task<JSONObject> getDocumentList(final int offset, final int limit, final Session session) {
        final String url = mBaseUri.buildUpon().path("/documents")
                .appendQueryParameter("offset", Integer.toString(offset))
                .appendQueryParameter("limit", Integer.toString(limit)).toString();
        return doRequestWithJsonResponse(url, GET, session);
    }

    public Task<JSONObject> searchDocuments(final String searchTerm, @Nullable final String docType, final int offset,
                                            final int limit, final Session session) {
        final Uri.Builder url = mBaseUri.buildUpon().path("/search").appendQueryParameter("q", searchTerm)
                .appendQueryParameter("offset", Integer.toString(offset))
                .appendQueryParameter("limit", Integer.toString(limit));
        if (docType != null) {
            url.appendQueryParameter("docType", docType);
        }
        return doRequestWithJsonResponse(url.toString(), GET, checkNotNull(session));
    }

    /**
     * Helper method to do a request that returns JSON data. The request is wrapped in a Task that will resolve to a
     * JSONObject.
     *
     * @param url       The full URL of the request.
     * @param method    The HTTP method of the request.
     * @param session   A valid session for the Gini API.
     * @return          A Task which will resolve to a JSONObject representing the response of the Gini API.
     */
    private Task<JSONObject> doRequestWithJsonResponse(final String url, int method, final Session session) {
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final BearerJsonObjectRequest documentsRequest =
                new BearerJsonObjectRequest(method, url, null, checkNotNull(session), completionSource, completionSource);
        mRequestQueue.add(documentsRequest);
        return completionSource.getTask();
    }

    private Uri uriRelativeToBaseUri(Uri uri) {

        return mBaseUri.buildUpon().path(uri.getPath()).query(uri.getQuery()).build();
    }

    public enum PreviewSize {
        /** Medium sized image, maximum dimensions are 750x900. */
        MEDIUM("750x900"),
        /** Big image, maximum dimensions are 1280x1810 */
        BIG("1280x1810");

        private final String mDimensions;

        PreviewSize(final String dimensions) {
            mDimensions = dimensions;
        }

        public String getDimensions() {
            return mDimensions;
        }
    }
}
