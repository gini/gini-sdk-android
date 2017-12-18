package net.gini.android.authorization;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


public final class PubKeyManager implements X509TrustManager {

    private X509Certificate[] mLocalCertificates;

    public PubKeyManager(@NotNull X509Certificate[] certificates) {
        mLocalCertificates = certificates;
    }

    @Override
    public void checkServerTrusted(X509Certificate[] remoteX509Certificates, String authType)
            throws CertificateException {
        if (remoteX509Certificates == null) {
            throw new IllegalArgumentException("checkServerTrusted: Remote X509Certificate array is null");
        }
        if (remoteX509Certificates.length == 0) {
            throw new IllegalArgumentException("checkServerTrusted: Remote X509Certificate array is empty");
        }

        checkSSLTLSFor(remoteX509Certificates, authType);

        boolean trusted = false;
        for (X509Certificate remoteCert : remoteX509Certificates) {
            if (isValidCertificate(remoteCert)) {
                trusted = true;
                break;
            }
        }

        if (!trusted) {
            throw new CertificateException("Remote certificate not trusted");
        }
    }

    private void checkSSLTLSFor(X509Certificate[] certificates, String authType)
            throws CertificateException {
        TrustManagerFactory tmf;
        try {
            tmf = TrustManagerFactory.getInstance("X509");
            tmf.init((KeyStore) null);

            for (TrustManager trustManager : tmf.getTrustManagers()) {
                ((X509TrustManager) trustManager).checkServerTrusted(
                        certificates, authType);
            }

        } catch (Exception e) {
            throw new CertificateException(e.toString());
        }
    }

    private String getEncodedPublicKeyFrom(X509Certificate certificate) {
        PublicKey publicKey = certificate.getPublicKey();
        return new BigInteger(1, publicKey.getEncoded()).toString(16);
    }

    private Boolean isValidCertificate(X509Certificate remoteCertificate) {
        String remoteEncoded = getEncodedPublicKeyFrom(remoteCertificate);
        Boolean isValid = false;
        for (X509Certificate localCert : mLocalCertificates) {
            String localEncoded = getEncodedPublicKeyFrom(localCert);
            if (remoteEncoded.equalsIgnoreCase(localEncoded)) {
                isValid = true;
                break;
            }
        }
        return isValid;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
            throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}