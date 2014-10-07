package net.gini.android.authorization;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;


/**
 * The session is the value object for the session of a user.
 */
public class Session {
    final String mAccessToken;
    final Calendar mExpirationDate;

    public Session(final String accessToken, final Calendar expirationDate) {
        mAccessToken = accessToken;
        mExpirationDate = expirationDate;
    }

    /** The session's access token. */
    public String getAccessToken() {
        return mAccessToken;
    }

    /** The expiration date of the acces token. */
    public Calendar getExpirationDate() {
        return mExpirationDate;
    }

    /**
     * Uses the current locale's time to check whether or not this session has already expired.
     *
     * @return Whether or not the session has already expired.
     */
    public boolean hasExpired() {
        Calendar now = Calendar.getInstance();
        return now.after(mExpirationDate);
    }

    // TODO: exception encapsulation instead of simply throwing JSONException
    public static Session newSessionfromAPIResponse(final JSONObject apiResponse) throws JSONException {
        final String accessToken = apiResponse.getString("access_token");
        final Calendar now = Calendar.getInstance();
        final long expirationTime = now.getTimeInMillis() + apiResponse.getInt("expires_in") * 1000;
        // I am really sorry. But the calendar API seems to love mutable objects.
        now.setTimeInMillis(expirationTime);
        return new Session(accessToken, now);
    }
}
