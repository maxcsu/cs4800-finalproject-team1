/*
 * AI Assistance Disclosure:
 * Portions of this file were drafted with assistance from OpenAI ChatGPT.
 * All architecture, design, and final review were performed by Maxwell Nield.
 */

package edu.csu.javatron.server.net;

import edu.csu.javatron.server.ServerConfig;
import edu.csu.javatron.server.lobby.LobbyManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class ServerTransport {

    private final ServerConfig config;
    private final ServerConfig.Logger logger;
    private final LobbyManager lobbyManager;

    private ServerSocket serverSocket;
    private Thread acceptThread;

    public ServerTransport(ServerConfig config, ServerConfig.Logger logger, LobbyManager lobbyManager) {
        this.config = config;
        this.logger = logger;
        this.lobbyManager = lobbyManager;
    }

    public void start(int port) {
        if (acceptThread != null) return;

        acceptThread = new Thread(this::runAcceptLoop, "ServerAcceptLoop");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public void shutdown() {
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    private void runAcceptLoop() {
        try {
            InetAddress bindAddr = InetAddress.getByName(config.serverIp);
            serverSocket = new ServerSocket(config.serverPort, 50, bindAddr);
            logger.info("[Lobby] Server socket opened.");

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                lobbyManager.onClientConnected(socket);
            }
        } catch (IOException io) {
            logger.error("Accept loop stopped.", io);
        }
    }
}
