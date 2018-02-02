package net.gini.android.authorization;

import android.util.Base64;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


public final class PubKeyManager implements X509TrustManager {

    private final X509Certificate[] mLocalCertificates;
    private final String[] mLocalPublicKeys;

    public static Builder builder() {
        return new Builder();
    }

    private PubKeyManager(@NotNull final X509Certificate[] certificates,
            @NotNull final String[] publicKeys) {
        mLocalCertificates = certificates;
        mLocalPublicKeys = publicKeys;
    }

    private PubKeyManager(@NotNull final X509Certificate[] certificates) {
        mLocalCertificates = certificates;
        mLocalPublicKeys = new String[0];
    }

    private PubKeyManager(@NotNull final String[] publicKeys) {
        mLocalCertificates = new X509Certificate[0];
        mLocalPublicKeys = publicKeys;
    }

    @Override
    public void checkServerTrusted(X509Certificate[] remoteX509Certificates, String authType)
            throws CertificateException {
        if (remoteX509Certificates == null) {
            throw new IllegalArgumentException(
                    "checkServerTrusted: Remote X509Certificate array is null");
        }
        if (remoteX509Certificates.length == 0) {
            throw new IllegalArgumentException(
                    "checkServerTrusted: Remote X509Certificate array is empty");
        }

        checkSSLTLS(remoteX509Certificates, authType);

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

    private void checkSSLTLS(X509Certificate[] certificates, String authType)
            throws CertificateException {
        TrustManagerFactory trustManagerFactory;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance("X509");
            trustManagerFactory.init((KeyStore) null);

            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                ((X509TrustManager) trustManager).checkServerTrusted(
                        certificates, authType);
            }

        } catch (Exception e) {
            throw new CertificateException(e.toString());
        }
    }

    private String getEncodedPublicKey(X509Certificate certificate) {
        /* DER encoded Public Keys start with 0x30 (ASN.1 SEQUENCE and CONSTRUCTED), so there is
         no leading 0x00 to drop.*/
        PublicKey publicKey = certificate.getPublicKey();
        return new BigInteger(1, publicKey.getEncoded()).toString(16);
    }

    private String getEncodedPublicKey(String publicKeyString) throws CertificateException {
        /* DER encoded Public Keys start with 0x30 (ASN.1 SEQUENCE and CONSTRUCTED), so there is
         no leading 0x00 to drop.*/
        PublicKey publicKey = makePublicKey(publicKeyString);
        return new BigInteger(1, publicKey.getEncoded()).toString(16);
    }

    private PublicKey makePublicKey(final String publicKeyString) throws CertificateException {
        try {
            String pubKeyString = publicKeyString.replaceAll("-----.*?-----","");
            byte[] keyBytes = Base64.decode(pubKeyString.getBytes(), Base64.DEFAULT);
            X509EncodedKeySpec X509keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(X509keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CertificateException(e);
        }
    }

    private Boolean isValidCertificate(X509Certificate remoteCertificate)
            throws CertificateException {
        String remotePublicKey = getEncodedPublicKey(remoteCertificate);
        for (X509Certificate localCert : mLocalCertificates) {
            String localPublicKey = getEncodedPublicKey(localCert);
            if (remotePublicKey.equalsIgnoreCase(localPublicKey)) {
                return true;
            }
        }
        for (String localPublicKeyString : mLocalPublicKeys) {
            String localPublicKey = getEncodedPublicKey(localPublicKeyString);
            if (remotePublicKey.equalsIgnoreCase(localPublicKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
            throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    public static class Builder {

        private X509Certificate[] mLocalCertificates;
        private String[] mLocalPublicKeys;

        Builder() {
        }

        public Builder setLocalCertificates(final X509Certificate[] localCertificates) {
            mLocalCertificates = localCertificates;
            return this;
        }

        public Builder setLocalPublicKeys(final String[] localPublicKeys) {
            mLocalPublicKeys = localPublicKeys;
            return this;
        }

        public boolean canBuild() {
            return mLocalCertificates != null || mLocalPublicKeys != null;
        }

        public PubKeyManager build() {
            if (mLocalCertificates != null && mLocalPublicKeys != null) {
                return new PubKeyManager(mLocalCertificates, mLocalPublicKeys);
            } else if (mLocalCertificates != null) {
                return new PubKeyManager(mLocalCertificates);
            } else if (mLocalPublicKeys != null) {
                return new PubKeyManager(mLocalPublicKeys);
            }
            throw new IllegalArgumentException(
                    "Cannot create PubKeyManager: no local certificates or public keys were set.");
        }
    }
}