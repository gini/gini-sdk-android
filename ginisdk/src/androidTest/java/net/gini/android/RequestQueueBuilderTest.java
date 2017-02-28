package net.gini.android;

import android.test.AndroidTestCase;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.NoCache;

public class RequestQueueBuilderTest extends AndroidTestCase {

    public void testCreateDefaultRequestQueue() throws Exception {
        RequestQueueBuilder requestQueueBuilder = new RequestQueueBuilder(getContext());
        RequestQueue requestQueue = requestQueueBuilder.build();

        assertNotNull(requestQueue);
    }

    public void testCacheConfiguration() {
        RequestQueueBuilder requestQueueBuilder = new RequestQueueBuilder(getContext());
        NoCache cache = new NoCache();
        RequestQueue requestQueue = requestQueueBuilder
                .setCache(cache)
                .build();

        assertSame(cache, requestQueue.getCache());
    }

}
