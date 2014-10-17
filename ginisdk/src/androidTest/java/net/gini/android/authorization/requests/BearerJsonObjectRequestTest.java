package net.gini.android.authorization.requests;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;

import junit.framework.TestCase;

import net.gini.android.authorization.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Map;

public class BearerJsonObjectRequestTest extends TestCase {
    public void testAcceptHeader() throws AuthFailureError {
        Session session = new Session("1234-5678-9012", Calendar.getInstance());
        BearerJsonObjectRequest request = new BearerJsonObjectRequest(Request.Method.GET, "https://example.com", null, session, null, null);

        Map<String, String> headers = request.getHeaders();
        assertEquals("application/json", headers.get("Accept"));
    }

    public void testContentTypeHeader() throws AuthFailureError, JSONException {
        Session session = new Session("1234-5678-9012", Calendar.getInstance());
        JSONObject payload = new JSONObject();
        payload.put("foo", "bar");
        BearerJsonObjectRequest request = new BearerJsonObjectRequest(Request.Method.GET, "https://example.com", payload, session, null, null);

        assertEquals("application/json; charset=utf-8", request.getBodyContentType());
    }
}
