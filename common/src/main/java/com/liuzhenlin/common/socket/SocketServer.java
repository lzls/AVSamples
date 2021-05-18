package com.liuzhenlin.common.socket;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.common.utils.Synthetic;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SocketServer implements Socket {

    private final int mPort;
    private WebSocketServer mWebSocketServer;
    @Synthetic WebSocket mWebSocket;
    @Synthetic Callback mCallback;

    public SocketServer(int port) {
        mPort = port;
    }

    public void setCallback(@Nullable Callback callback) {
        mCallback = callback;
    }

    public void start() {
        mWebSocketServer = new WebSocketServer(new InetSocketAddress(mPort)) {
            @Override
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
                mWebSocket = webSocket;
            }

            @Override
            public void onClose(WebSocket webSocket, int i, String s, boolean b) {
                mWebSocket = null;
            }

            @Override
            public void onMessage(WebSocket webSocket, String s) {
            }

            @Override
            public void onMessage(WebSocket conn, ByteBuffer message) {
                byte[] buff = new byte[message.remaining()];
                message.get(buff);
                if (mCallback != null) {
                    mCallback.onData(buff);
                }
            }

            @Override
            public void onError(WebSocket webSocket, Exception e) {
            }

            @Override
            public void onStart() {
            }
        };
        mWebSocketServer.start();
    }

    public void stop() {
        try {
            if (mWebSocket != null) {
                mWebSocket.close();
            }
            mWebSocketServer.stop();
            mWebSocketServer = null;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendData(@NonNull byte[] bytes) {
        if (mWebSocket != null && mWebSocket.isOpen()) {
            mWebSocket.send(bytes);
        }
    }
}
