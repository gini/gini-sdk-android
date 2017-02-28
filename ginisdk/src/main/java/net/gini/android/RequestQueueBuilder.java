package net.gini.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

import java.io.File;

/**
 * <p>
 * Helper class for creating com.android.volley.RequestQueue instances.
 * </p>
 * <p>
 * The default dependencies are taken from the Volley.newRequestQueue() implementation.
 * </p>
 * <p>
 * If no dependency instances were set the builder simply returns Volley.newRequestQueue()'s result.
 * </p>
 */
class RequestQueueBuilder {

    /** Default on-disk cache directory. */
    private static final String DEFAULT_CACHE_DIR = "volley";

    private final Context mContext;

    private Cache mCache;
    private String mUserAgent;
    private HttpStack mStack;
    private Network mNetwork;

    RequestQueueBuilder(final Context context) {
        mContext = context;
    }

    RequestQueueBuilder setCache(final Cache cache) {
        mCache = cache;
        return this;
    }

    RequestQueue build() {
        if (mCache == null && mUserAgent == null && mStack == null && mNetwork == null) {
            return Volley.newRequestQueue(mContext);
        } else {
            return new RequestQueue(getCache(), getNetwork());
        }
    }

    private Cache getCache() {
        if (mCache == null) {
            File cacheDir = new File(mContext.getCacheDir(), DEFAULT_CACHE_DIR);
            mCache = new DiskBasedCache(cacheDir);
        }
        return mCache;
    }

    private String getUserAgent() {
        if (mUserAgent == null) {
            mUserAgent = "volley/0";
            try {
                String packageName = mContext.getPackageName();
                PackageInfo info = mContext.getPackageManager().getPackageInfo(packageName, 0);
                mUserAgent = packageName + "/" + info.versionCode;
            } catch (PackageManager.NameNotFoundException ignore) {
            }
        }
        return mUserAgent;
    }

    private HttpStack getStack() {
        if (mStack == null) {
            if (Build.VERSION.SDK_INT >= 9) {
                mStack = new HurlStack();
            } else {
                // Prior to Gingerbread, HttpUrlConnection was unreliable.
                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                mStack = new HttpClientStack(AndroidHttpClient.newInstance(getUserAgent()));
            }
        }
        return mStack;
    }

    private Network getNetwork() {
        if (mNetwork == null) {
            mNetwork = new BasicNetwork(getStack());
        }
        return mNetwork;
    }
}
