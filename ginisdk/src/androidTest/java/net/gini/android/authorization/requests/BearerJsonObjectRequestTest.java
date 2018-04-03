package net.gini.android.authorization.requests;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RetryPolicy;

import junit.framework.TestCase;

import net.gini.android.MediaTypes;
import net.gini.android.authorization.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Map;

public class BearerJsonObjectRequestTest extends TestCase {

    private RetryPolicy retryPolicy;

    @Override
    protected void setUp() throws Exception {
        retryPolicy = new DefaultRetryPolicy();
    }

    public void testAcceptHeader() throws AuthFailureError {
        Session session = new Session("1234-5678-9012", new Date());
        BearerJsonObjectRequest request = new BearerJsonObjectRequest(Request.Method.GET, "https://example.com", null, session, null, null, retryPolicy);

        Map<String, String> headers = request.getHeaders();
        assertEquals("application/json, application/vnd.gini.v2+json", headers.get("Accept"));
    }

    public void testContentTypeHeader() throws AuthFailureError, JSONException {
        Session session = new Session("1234-5678-9012", new Date());
        JSONObject payload = new JSONObject();
        payload.put("foo", "bar");
        BearerJsonObjectRequest request = new BearerJsonObjectRequest(Request.Method.GET, "https://example.com", payload, session, null, null, retryPolicy);

        assertEquals("application/json; charset=utf-8", request.getBodyContentType());
    }

    public void testCustomContentTypeHeader() throws AuthFailureError, JSONException {
        Session session = new Session("1234-5678-9012", new Date());
        JSONObject payload = new JSONObject();
        payload.put("foo", "bar");
        BearerJsonObjectRequest request = new BearerJsonObjectRequest(Request.Method.GET, "https://example.com", payload, session, null, null, retryPolicy, MediaTypes.GINI_JSON_V2);

        assertEquals(MediaTypes.GINI_JSON_V2, request.getBodyContentType());
    }
}
