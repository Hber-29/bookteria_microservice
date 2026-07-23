package com.devteria.chat.controller;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SocketHandle {
    SocketIOServer socketIOServer;

    @OnConnect
    public void clientConnected(SocketIOClient socketIOClient) {
        log.info("Client connected: {}",socketIOClient.getSessionId());
    }
    @OnDisconnect
    public void clientDisconnected(SocketIOClient socketIOClient) {
        log.info("Client disconnected: {}",socketIOClient.getSessionId());
    }
    @PostConstruct
    public void startServer(){
        socketIOServer.start();
        socketIOServer.addListeners(this);
        log.info("Socket Server Started");
    }
    @PreDestroy
    public void stopServer(){
        socketIOServer.stop();
        log.info("Socket Server Stopped");
    }
}
