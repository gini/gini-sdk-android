package net.gini.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

class TLSPreferredSocket extends SSLSocket {

    private final SSLSocket mSSLSocket;

    TLSPreferredSocket(@NonNull SSLSocket mSSLSocket) {
        this.mSSLSocket = mSSLSocket;
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return mSSLSocket.getSupportedCipherSuites();
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return mSSLSocket.getEnabledCipherSuites();
    }

    @Override
    public void setEnabledCipherSuites(String[] strings) {
        mSSLSocket.setEnabledCipherSuites(strings);
    }

    @Override
    public String[] getSupportedProtocols() {
        return mSSLSocket.getSupportedProtocols();
    }

    @Override
    public String[] getEnabledProtocols() {
        return mSSLSocket.getEnabledProtocols();
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        final String[] onlyEnabledProtocols = filterOutNonEnabledProtocols(protocols);
        if (onlyEnabledProtocols == null || onlyEnabledProtocols.length == 0) {
            return;
        }
        protocols = filterOutSSLv3(onlyEnabledProtocols);
        if (protocols != null && protocols.length == 1 && "SSLv3".equals(protocols[0])) {
            protocols = filterOutSSLv3(getEnabledProtocols());
        }
        mSSLSocket.setEnabledProtocols(protocols);
    }

    @Nullable
    private String[] filterOutNonEnabledProtocols(@Nullable String[] protocols) {
        if (protocols == null || protocols.length == 0) {
            return protocols;
        }
        final List<String> filtered = new ArrayList<>();
        final String[] enabledProtocols = getEnabledProtocols();
        for (String protocol : protocols) {
            for (String enabledProtocol : enabledProtocols) {
                if (protocol.equals(enabledProtocol)) {
                    filtered.add(protocol);
                    break;
                }
            }
        }
        return filtered.toArray(new String[filtered.size()]);
    }

    @Nullable
    private String[] filterOutSSLv3(@Nullable String[] protocols) {
        if (protocols == null || protocols.length == 0) {
            return protocols;
        }
        boolean hasSSLv3 = false;
        int sslv3Count = 0;
        for (String protocol : protocols) {
            if ("SSLv3".equals(protocol)) {
                hasSSLv3 = true;
                sslv3Count++;
            }
        }
        if (hasSSLv3 && protocols.length == 1) {
            return protocols;
        }
        if (hasSSLv3) {
            final String[] filtered = new String[protocols.length - sslv3Count];
            int j = 0;
            for (String protocol : protocols) {
                if (!"SSLv3".equals(protocol)) {
                    filtered[j] = protocol;
                    j++;
                }
            }
            return filtered;
        }
        return protocols;
    }

    @Override
    public SSLSession getSession() {
        return mSSLSocket.getSession();
    }

    @Override
    public void addHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {
        mSSLSocket.addHandshakeCompletedListener(handshakeCompletedListener);
    }

    @Override
    public void removeHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {
        mSSLSocket.removeHandshakeCompletedListener(handshakeCompletedListener);
    }

    @Override
    public void startHandshake() throws IOException {
        mSSLSocket.startHandshake();
    }

    @Override
    public void setUseClientMode(boolean b) {
        mSSLSocket.setUseClientMode(b);
    }

    @Override
    public boolean getUseClientMode() {
        return mSSLSocket.getUseClientMode();
    }

    @Override
    public void setNeedClientAuth(boolean b) {
        mSSLSocket.setNeedClientAuth(b);
    }

    @Override
    public void setWantClientAuth(boolean b) {
        mSSLSocket.setWantClientAuth(b);
    }

    @Override
    public boolean getNeedClientAuth() {
        return mSSLSocket.getNeedClientAuth();
    }

    @Override
    public boolean getWantClientAuth() {
        return mSSLSocket.getWantClientAuth();
    }

    @Override
    public void setEnableSessionCreation(boolean b) {
        mSSLSocket.setEnableSessionCreation(b);
    }

    @Override
    public boolean getEnableSessionCreation() {
        return mSSLSocket.getEnableSessionCreation();
    }

    @Override
    public void shutdownInput() throws IOException {
        mSSLSocket.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        mSSLSocket.shutdownOutput();
    }

    @Override
    public SSLParameters getSSLParameters() {
        return mSSLSocket.getSSLParameters();
    }

    @Override
    public void setSSLParameters(SSLParameters p) {
        mSSLSocket.setSSLParameters(p);
    }

    @Override
    public synchronized void close() throws IOException {
        mSSLSocket.close();
    }

    @Override
    public InetAddress getInetAddress() {
        return mSSLSocket.getInetAddress();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return mSSLSocket.getInputStream();
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return mSSLSocket.getKeepAlive();
    }

    @Override
    public InetAddress getLocalAddress() {
        return mSSLSocket.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return mSSLSocket.getLocalPort();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return mSSLSocket.getOutputStream();
    }

    @Override
    public int getPort() {
        return mSSLSocket.getPort();
    }

    @Override
    public int getSoLinger() throws SocketException {
        return mSSLSocket.getSoLinger();
    }

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        return mSSLSocket.getReceiveBufferSize();
    }

    @Override
    public synchronized int getSendBufferSize() throws SocketException {
        return mSSLSocket.getSendBufferSize();
    }

    @Override
    public synchronized int getSoTimeout() throws SocketException {
        return mSSLSocket.getSoTimeout();
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return mSSLSocket.getTcpNoDelay();
    }

    @Override
    public void setKeepAlive(boolean keepAlive) throws SocketException {
        mSSLSocket.setKeepAlive(keepAlive);
    }

    @Override
    public synchronized void setSendBufferSize(int size) throws SocketException {
        mSSLSocket.setSendBufferSize(size);
    }

    @Override
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        mSSLSocket.setReceiveBufferSize(size);
    }

    @Override
    public void setSoLinger(boolean on, int timeout) throws SocketException {
        mSSLSocket.setSoLinger(on, timeout);
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        mSSLSocket.setSoTimeout(timeout);
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        mSSLSocket.setTcpNoDelay(on);
    }

    @Override
    public String toString() {
        return mSSLSocket.toString();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return mSSLSocket.getLocalSocketAddress();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return mSSLSocket.getRemoteSocketAddress();
    }

    @Override
    public boolean isBound() {
        return mSSLSocket.isBound();
    }

    @Override
    public boolean isConnected() {
        return mSSLSocket.isConnected();
    }

    @Override
    public boolean isClosed() {
        return mSSLSocket.isClosed();
    }

    @Override
    public void bind(SocketAddress localAddr) throws IOException {
        mSSLSocket.bind(localAddr);
    }

    @Override
    public void connect(SocketAddress remoteAddr) throws IOException {
        mSSLSocket.connect(remoteAddr);
    }

    @Override
    public void connect(SocketAddress remoteAddr, int timeout) throws IOException {
        mSSLSocket.connect(remoteAddr, timeout);
    }

    @Override
    public boolean isInputShutdown() {
        return mSSLSocket.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return mSSLSocket.isOutputShutdown();
    }

    @Override
    public void setReuseAddress(boolean reuse) throws SocketException {
        mSSLSocket.setReuseAddress(reuse);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return mSSLSocket.getReuseAddress();
    }

    @Override
    public void setOOBInline(boolean oobinline) throws SocketException {
        mSSLSocket.setOOBInline(oobinline);
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return mSSLSocket.getOOBInline();
    }

    @Override
    public void setTrafficClass(int value) throws SocketException {
        mSSLSocket.setTrafficClass(value);
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return mSSLSocket.getTrafficClass();
    }

    @Override
    public void sendUrgentData(int value) throws IOException {
        mSSLSocket.sendUrgentData(value);
    }

    @Override
    public SocketChannel getChannel() {
        return mSSLSocket.getChannel();
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        mSSLSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }
}
