package server;

import model.EmailManager;
import model.UserManager;
import utils.EmailUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailServer {
    private static final Logger LOGGER = Logger.getLogger(EmailServer.class.getName());


    private static final int CORE_POOL_SIZE = 10;
    private static final int MAX_POOL_SIZE = 50;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final int QUEUE_CAPACITY = 100;

    private final int port;
    private final UserManager userManager;
    private final EmailManager emailManager;
    private final Map<String, ClientHandler> activeClients;
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

        this.threadPool = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        this.running = false;
    }

    public void start() {
        running = true;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.serverSocket = serverSocket;
            LOGGER.info("Email server started on port " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    LOGGER.info("New client connection: " + clientSocket.getInetAddress());

                    ClientHandler clientHandler = new ClientHandler(clientSocket, emailManager, userManager, activeClients);
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
            try {
                if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
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