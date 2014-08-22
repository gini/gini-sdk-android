package net.gini.android.authorization;


import org.json.JSONObject;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class Session {
    final String mAccessToken;

    public Session(final String accessToken) {
        mAccessToken = accessToken;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public boolean hasExpired() {
        return true;
    }

    public static Future<Session> sessionFutureFromAPIResponse(final Future<JSONObject> apiResponse) {
        return new FutureTask<Session>(new Callable<Session>() {
            @Override
            public Session call() throws Exception {
                JSONObject responseData = apiResponse.get();
                String accessToken = responseData.getString("access_token");
                return  new Session(accessToken);
            }
        });
    }
}
