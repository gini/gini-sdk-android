package net.gini.android;


import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;

public class Utils {

    private Utils(){
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    /**
     * Creates an urlencoded string from the given data. The created string can be used as a query string or the request
     * body of a x-www-form-urlencoded form.
     *
     * @param data  A map where the key is the name of the query parameter and the value is the parameter's value.
     * @return      The urlencoded data.
     */
    public static String mapToUrlEncodedString(Map<String, String> data) {
        ArrayList<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>(data.size());
        for (Map.Entry<String, String> entry : data.entrySet()) {
            parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return URLEncodedUtils.format(parameters, "utf-8");
    }

    public static Charset CHARSET_UTF8 = Charset.forName("utf-8");
}
