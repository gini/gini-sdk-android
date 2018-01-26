package net.gini.android;

import android.os.Build;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

class TLSPreferredSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory mSSLSocketFactory;

    static boolean isTLSv1xSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    TLSPreferredSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        mSSLSocketFactory = createSSLSocketFactory(null);
    }

    TLSPreferredSocketFactory(TrustManager[] trustManagers) throws NoSuchAlgorithmException, KeyManagementException {
        mSSLSocketFactory = createSSLSocketFactory(trustManagers);
    }

    @NotNull
    private SSLSocketFactory createSSLSocketFactory(TrustManager[] trustManagers) throws KeyManagementException, NoSuchAlgorithmException {
        final SSLContext sslContext = createSSLContext(trustManagers);
        return sslContext.getSocketFactory();
    }

    @NotNull
    private SSLContext createSSLContext(TrustManager[] trustManagers) throws NoSuchAlgorithmException, KeyManagementException {
        if (TLSPreferredSocketFactory.isTLSv1xSupported()) {
            final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, trustManagers, null);
            return sslContext;
        }
        throw new NoSuchAlgorithmException();
    }


    @Override
    public String[] getDefaultCipherSuites() {
        return mSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return mSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
        return createTLSPreferredSocket(mSSLSocketFactory.createSocket(socket, s, i, b));
    }

    private Socket createTLSPreferredSocket(Socket socket) {
        if (socket instanceof SSLSocket) {
            return new TLSPreferredSocket((SSLSocket) socket);
        }
        return socket;
    }

    @Override
    public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
        return createTLSPreferredSocket(mSSLSocketFactory.createSocket(s, i));
    }

    @Override
    public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
        return createTLSPreferredSocket(mSSLSocketFactory.createSocket(s, i, inetAddress, i1));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
        return createTLSPreferredSocket(mSSLSocketFactory.createSocket(inetAddress, i));
    }

    @Override
    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
        return createTLSPreferredSocket(mSSLSocketFactory.createSocket(inetAddress, i, inetAddress1, i1));
    }
}
