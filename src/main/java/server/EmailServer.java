package server;

import model.EmailManager;
import model.UserManager;
import utils.EmailUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailServer {
    private static final Logger LOGGER = Logger.getLogger(EmailServer.class.getName());
    private static final int MAX_THREADS = 50;

    private final int port;
    private final UserManager userManager;
    private final EmailManager emailManager;
    private final Map<String, server.ClientHandler> activeClients;
    private final ExecutorService threadPool;
    private boolean running;
    private ServerSocket serverSocket;

    public EmailServer() {
        this(EmailUtils.PORT);
    }

    public EmailServer(int port) {
        this.port = port;
        this.userManager = new UserManager();
        this.emailManager = new EmailManager(userManager);
        this.activeClients = new ConcurrentHashMap<>();
        this.threadPool = Executors.newFixedThreadPool(MAX_THREADS);
        this.running = false;
    }

    public void start() {
        running = true;

        try {
            serverSocket = new ServerSocket(port);
            LOGGER.info("Email server started on port " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    LOGGER.info("New client connection: " + clientSocket.getInetAddress());

                    server.ClientHandler clientHandler = new server.ClientHandler(clientSocket, emailManager, userManager, activeClients);
                    threadPool.submit(clientHandler);
                } catch (IOException e) {
                    if (running) {
                        LOGGER.log(Level.SEVERE, "Error accepting client connection", e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error starting server on port " + port, e);
        } finally {
            shutdown();
        }
    }

    public void stop() {
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error closing server socket", e);
        }

        threadPool.shutdown();
        LOGGER.info("Server shutdown initiated");
    }

    private void shutdown() {
        if (!threadPool.isShutdown()) {
            threadPool.shutdown();
        }
        LOGGER.info("Server resources cleaned up");
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public static void main(String[] args) {
        EmailServer server = new EmailServer();
        server.setupShutdownHook();
        server.start();
    }
}