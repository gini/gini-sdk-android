package net.gini.android;

import com.android.volley.RequestQueue;

import net.gini.android.authorization.Session;
import net.gini.android.requests.BearerUploadRequest;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.HashMap;

import bolts.Task;

import static com.android.volley.Request.Method.POST;
import static net.gini.android.Utils.checkNotNull;
import static net.gini.android.Utils.mapToUrlEncodedString;

public class ApiCommunicator {

    private final String mBaseUrl;
    private final RequestQueue mRequestQueue;


    public ApiCommunicator(final String mBaseUrl, final RequestQueue mRequestQueue) {
        this.mBaseUrl = checkNotNull(mBaseUrl);
        this.mRequestQueue = checkNotNull(mRequestQueue);
    }

    public Task<JSONObject> uploadDocument(final byte[] documentData, final String contentType,
                                           @Nullable final String documentName, @Nullable final String docTypeHint,
                                           final Session session) {

        final HashMap<String, String> requestQueryData = new HashMap<String, String>();
        if (documentName != null) {
            requestQueryData.put("filename", documentName);
        }
        if (docTypeHint != null) {
            requestQueryData.put("doctype", docTypeHint);
        }
        final String url = mBaseUrl + "documents/?" + mapToUrlEncodedString(requestQueryData);
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final BearerUploadRequest request =
                new BearerUploadRequest(POST, url, checkNotNull(documentData), checkNotNull(contentType),
                                        session.getAccessToken(), completionSource, completionSource);
        mRequestQueue.add(request);

        return completionSource.getTask();
    }
}
