package net.gini.android;

import android.support.test.filters.SmallTest;
import android.test.AndroidTestCase;

import java.io.IOException;
import java.util.Arrays;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

@SmallTest
public class TLSPreferredSocketTest extends AndroidTestCase {

    public void testSSLv3RemovalFromProtocols() {
        SSLSocketProtocolsStub mSSLSocket = new SSLSocketProtocolsStub();
        mSSLSocket.setEnabledProtocols(new String[]{"TLS", "TLSv1", "SSLv3", "TLSv1.1", "TLSv1.2"});

        TLSPreferredSocket tlsPreferredSocket = new TLSPreferredSocket(mSSLSocket);
        tlsPreferredSocket.setEnabledProtocols(new String[]{"SSLv3"});

        assertTrue(Arrays.equals(new String[]{"TLS", "TLSv1", "TLSv1.1", "TLSv1.2"}, tlsPreferredSocket.getEnabledProtocols()));
    }

    public void testSSLv3RemovalFromProtocolsWithMultipleSSLv3Entries() {
        SSLSocketProtocolsStub mSSLSocket = new SSLSocketProtocolsStub();
        mSSLSocket.setEnabledProtocols(new String[]{"SSLv3", "TLS", "TLSv1", "SSLv3", "TLSv1.1", "SSLv3", "TLSv1.2"});

        TLSPreferredSocket tlsPreferredSocket = new TLSPreferredSocket(mSSLSocket);
        tlsPreferredSocket.setEnabledProtocols(new String[]{"SSLv3", "TLS", "SSLv3"});

        assertTrue(Arrays.equals(new String[]{"TLS"}, tlsPreferredSocket.getEnabledProtocols()));
    }

    public void testSSLv3RemovalFromProtocolsWithNoSSLv3Entries() {
        SSLSocketProtocolsStub mSSLSocket = new SSLSocketProtocolsStub();
        mSSLSocket.setEnabledProtocols(new String[]{"TLS", "TLSv1", "TLSv1.1", "TLSv1.2"});

        TLSPreferredSocket tlsPreferredSocket = new TLSPreferredSocket(mSSLSocket);
        tlsPreferredSocket.setEnabledProtocols(new String[]{"TLSv1.1", "TLSv1.2"});

        assertTrue(Arrays.equals(new String[]{"TLSv1.1", "TLSv1.2"}, tlsPreferredSocket.getEnabledProtocols()));
    }

    public void testSSLv3RemovalFromProtocolsWithOnlySSLv3Enabled() {
        SSLSocketProtocolsStub mSSLSocket = new SSLSocketProtocolsStub();
        mSSLSocket.setEnabledProtocols(new String[]{"SSLv3"});

        TLSPreferredSocket tlsPreferredSocket = new TLSPreferredSocket(mSSLSocket);
        tlsPreferredSocket.setEnabledProtocols(new String[]{"TLSv1.1", "TLSv1.2"});

        assertTrue(Arrays.equals(new String[]{"SSLv3"}, tlsPreferredSocket.getEnabledProtocols()));
    }

    public void testSSLv3RemovalFromProtocolsWithFilteringOutNonEnabledProtocols() {
        SSLSocketProtocolsStub mSSLSocket = new SSLSocketProtocolsStub();
        mSSLSocket.setEnabledProtocols(new String[]{"TLSv1.1"});

        TLSPreferredSocket tlsPreferredSocket = new TLSPreferredSocket(mSSLSocket);
        tlsPreferredSocket.setEnabledProtocols(new String[]{"TLSv1.1", "TLSv1.2"});

        assertTrue(Arrays.equals(new String[]{"TLSv1.1"}, tlsPreferredSocket.getEnabledProtocols()));
    }

    public void testSSLv3RemovalFromProtocolsWithEmptyProtocols() {
        SSLSocketProtocolsStub mSSLSocket = new SSLSocketProtocolsStub();
        mSSLSocket.setEnabledProtocols(new String[]{"SSLv3"});

        TLSPreferredSocket tlsPreferredSocket = new TLSPreferredSocket(mSSLSocket);
        tlsPreferredSocket.setEnabledProtocols(new String[0]);

        assertTrue(Arrays.equals(new String[]{"SSLv3"}, tlsPreferredSocket.getEnabledProtocols()));
    }

    public void testSSLv3RemovalFromProtocolsWithNullProtocolArrays() {
        SSLSocketProtocolsStub mSSLSocket = new SSLSocketProtocolsStub();
        mSSLSocket.setEnabledProtocols(null);

        TLSPreferredSocket tlsPreferredSocket = new TLSPreferredSocket(mSSLSocket);
        tlsPreferredSocket.setEnabledProtocols(null);

        assertNull(tlsPreferredSocket.getEnabledProtocols());
    }

    public void testSSLv3RemovalFromProtocolsWithOnlySSLv3() {
        SSLSocketProtocolsStub mSSLSocket = new SSLSocketProtocolsStub();
        mSSLSocket.setEnabledProtocols(new String[]{"SSLv3"});

        TLSPreferredSocket tlsPreferredSocket = new TLSPreferredSocket(mSSLSocket);
        tlsPreferredSocket.setEnabledProtocols(new String[]{"SSLv3"});

        assertTrue(Arrays.equals(new String[]{"SSLv3"}, tlsPreferredSocket.getEnabledProtocols()));
    }

    private static class SSLSocketProtocolsStub extends SSLSocket {

        private String[] mProtocols;

        @Override
        public String[] getSupportedCipherSuites() {
            return new String[0];
        }

        @Override
        public String[] getEnabledCipherSuites() {
            return new String[0];
        }

        @Override
        public void setEnabledCipherSuites(String[] strings) {

        }

        @Override
        public String[] getSupportedProtocols() {
            return new String[0];
        }

        @Override
        public String[] getEnabledProtocols() {
            return mProtocols;
        }

        @Override
        public void setEnabledProtocols(String[] strings) {
            mProtocols = strings;
        }

        @Override
        public SSLSession getSession() {
            return null;
        }

        @Override
        public void addHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {

        }

        @Override
        public void removeHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {

        }

        @Override
        public void startHandshake() throws IOException {

        }

        @Override
        public void setUseClientMode(boolean b) {

        }

        @Override
        public boolean getUseClientMode() {
            return false;
        }

        @Override
        public void setNeedClientAuth(boolean b) {

        }

        @Override
        public void setWantClientAuth(boolean b) {

        }

        @Override
        public boolean getNeedClientAuth() {
            return false;
        }

        @Override
        public boolean getWantClientAuth() {
            return false;
        }

        @Override
        public void setEnableSessionCreation(boolean b) {

        }

        @Override
        public boolean getEnableSessionCreation() {
            return false;
        }
    }
}