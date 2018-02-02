package net.gini.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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

import net.gini.android.authorization.PubKeyManager;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

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

    /**
     * Default on-disk cache directory.
     */
    private static final String DEFAULT_CACHE_DIR = "volley";

    private final Context mContext;

    private Cache mCache;
    private String mUserAgent;
    private HttpStack mStack;
    private Network mNetwork;
    private SSLSocketFactory mSSLSocketFactory;
    private String[] mCertificatePaths;
    private String[] mPubKeyPaths;

    RequestQueueBuilder(final Context context) {
        mContext = context;
    }

    void setCertificatePaths(final String[] certificatePaths) {
        mCertificatePaths = certificatePaths;
    }

    void setPublicKeyPaths(final String[] publicKeyPaths) {
        mPubKeyPaths = publicKeyPaths;
    }

    RequestQueueBuilder setCache(final Cache cache) {
        mCache = cache;
        return this;
    }

    RequestQueue build() {
        RequestQueue queue = new RequestQueue(getCache(), getNetwork());
        queue.start();
        return queue;
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
                mStack = getHurlStack();
            } else {
                // Prior to Gingerbread, HttpUrlConnection was unreliable.
                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                mStack = new HttpClientStack(AndroidHttpClient.newInstance(getUserAgent()));
            }
        }
        return mStack;
    }

    private HurlStack getHurlStack() {
        SSLSocketFactory sslSocketFactory = getSSLSocketFactory();
        if (sslSocketFactory != null) {
            return new HurlStack(null, sslSocketFactory);
        }
        return new HurlStack();
    }

    private SSLSocketFactory getSSLSocketFactory() {
        if (mSSLSocketFactory == null) {
            TrustManager[] trustManagers = getTrustManagers();
            if (trustManagers != null) {
                if (TLSPreferredSocketFactory.isTLSv1xSupported()) {
                    try {
                        mSSLSocketFactory = new TLSPreferredSocketFactory(trustManagers);
                    } catch (NoSuchAlgorithmException | KeyManagementException ignore) {
                    }
                } else {
                    try {
                        SSLContext sslContext = SSLContext.getDefault();
                        sslContext.init(null, trustManagers, null);
                        mSSLSocketFactory = sslContext.getSocketFactory();
                    } catch (NoSuchAlgorithmException | KeyManagementException ignore) {
                    }
                }
            } else if (TLSPreferredSocketFactory.isTLSv1xSupported()) {
                try {
                    mSSLSocketFactory = new TLSPreferredSocketFactory();
                } catch (NoSuchAlgorithmException | KeyManagementException ignore) {
                }
            }
        }
        return mSSLSocketFactory;
    }

    private TrustManager[] getTrustManagers() {
        final PubKeyManager.Builder builder = PubKeyManager.builder();
        if (mCertificatePaths != null && mCertificatePaths.length > 0) {
            builder.setLocalCertificates(getLocalCertificatesFromAssets(mCertificatePaths));

        }
        if (mPubKeyPaths != null && mPubKeyPaths.length > 0) {
            builder.setLocalPublicKeys(getLocalPublicKeysFromAssets(mPubKeyPaths));
        }
        if (builder.canBuild()) {
            final PubKeyManager pubKeyManager = builder.build();
            return new TrustManager[]{pubKeyManager};
        }
        return null;
    }

    private Network getNetwork() {
        if (mNetwork == null) {
            mNetwork = new BasicNetwork(getStack());
        }
        return mNetwork;
    }

    /**
     * Helper method to get local certificates from assets
     *
     * @param certFilePaths An array containing all certificates paths relatively to the assets
     * @return Local certificates
     * @throws IllegalArgumentException if the the certificate is not found or it is invalid.
     */
    private synchronized X509Certificate[] getLocalCertificatesFromAssets(String[] certFilePaths) {
        List<X509Certificate> certificates = new ArrayList<>();
        AssetManager assetManager = mContext.getAssets();
        for (String fileName : certFilePaths) {
            InputStream inputStream = null;
            try {
                inputStream = assetManager.open(fileName);
                X509Certificate certificate = createCertificate(inputStream);
                if (certificate != null) {
                    certificates.add(certificate);
                }
            } catch (IOException | CertificateException e) {
                throw new IllegalArgumentException(
                        "It is not a valid certificate or it does not exist in the assets: ",
                        e.getCause());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
        return certificates.toArray(new X509Certificate[certificates.size()]);
    }

    /**
     * Helper method to create certificate from and InputStream
     *
     * @param inputStream Certificate generated with the input stream
     * @return Generated cetificate
     * @throws IOException          if the the certificate is not found or it is invalid.
     * @throws CertificateException if parsing problems are detected when generating Certificate
     */
    private synchronized X509Certificate createCertificate(InputStream inputStream)
            throws IOException, CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        BufferedInputStream bis = new BufferedInputStream(inputStream);

        if (bis.available() > 0) {
            return (X509Certificate) cf.generateCertificate(bis);
        } else {
            return null;
        }
    }

    /**
     * Helper method to get local public keys from assets
     *
     * @param publicKeyPaths An array containing all public key paths relatively to the assets
     * @return Local certificates
     * @throws IllegalArgumentException if the the certificate is not found or it is invalid.
     */
    private synchronized String[] getLocalPublicKeysFromAssets(String[] publicKeyPaths) {
        List<String> publicKeys = new ArrayList<>();
        AssetManager assetManager = mContext.getAssets();
        for (String fileName : publicKeyPaths) {
            InputStream inputStream = null;
            try {
                inputStream = new BufferedInputStream(assetManager.open(fileName));
                String publicKey = readPublicKeyFile(inputStream);
                if (publicKey != null) {
                    publicKeys.add(publicKey);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "It is not a valid public key or it does not exist in the assets: ",
                        e.getCause());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }

        return publicKeys.toArray(new String[publicKeys.size()]);
    }

    private String readPublicKeyFile(final InputStream inputStream) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        try {
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString();
        } catch (IOException ignored) {
        }
        return null;
    }
}
