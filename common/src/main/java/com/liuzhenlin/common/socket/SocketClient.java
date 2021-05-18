package com.liuzhenlin.common.socket;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.common.utils.Synthetic;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class SocketClient implements Socket {

    private MyWebSocketClient mWebSocketClient;
    private final int mPort;
    @Synthetic Callback mCallback;

    public SocketClient(int port) {
        mPort = port;
    }

    public void setCallback(@Nullable Callback callback) {
        mCallback = callback;
    }

    public void connect(@NonNull String ip) {
        try {
            URI url = new URI("ws://" + ip + ":" + mPort);
            mWebSocketClient = new MyWebSocketClient(url);
            mWebSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        mWebSocketClient.close();
        mWebSocketClient = null;
    }

    @Override
    public void sendData(@NonNull byte[] bytes) {
        if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
            mWebSocketClient.send(bytes);
        }
    }

    private class MyWebSocketClient extends WebSocketClient {

        public MyWebSocketClient(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
        }

        @Override
        public void onMessage(String s) {
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            byte[] buff = new byte[bytes.remaining()];
            bytes.get(buff);
            if (mCallback != null) {
                mCallback.onData(buff);
            }
        }

        @Override
        public void onClose(int i, String s, boolean b) {
        }

        @Override
        public void onError(Exception e) {
        }
    }
}
