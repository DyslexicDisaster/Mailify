package server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import model.Email;
import model.EmailManager;
import model.PasswordHasher;
import model.UserManager;
import network.NetworkLayerJSON;
import utils.EmailUtils;

import java.io.IOException;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private static final Gson gson = new Gson();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Socket clientSocket;
    private final EmailManager emailManager;
    private final UserManager userManager;
    private final Map<String, ClientHandler> activeClients;
    private NetworkLayerJSON networkLayer;

    private String authenticatedUser = null;
    private boolean sessionActive = true;

    public ClientHandler(Socket clientSocket, EmailManager emailManager, UserManager userManager,
                         Map<String, ClientHandler> activeClients) {
        this.clientSocket = clientSocket;
        this.emailManager = emailManager;
        this.userManager = userManager;
        this.activeClients = activeClients;
    }

    @Override
    public void run() {
        try {
            networkLayer = new NetworkLayerJSON(clientSocket);
            LOGGER.info("Client connected: " + clientSocket.getInetAddress());

            while (sessionActive) {
                String request = networkLayer.receive();
                LOGGER.info("Received request: " + request);

                try {
                    JsonObject jsonRequest = JsonParser.parseString(request).getAsJsonObject();
                    processRequest(jsonRequest);
                } catch (JsonParseException e) {
                    sendErrorResponse("Invalid JSON format");
                    LOGGER.log(Level.WARNING, "Invalid JSON received", e);
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error handling client connection", e);
        } finally {
            cleanup();
        }
    }

    private void processRequest(JsonObject jsonRequest) {
        if (!jsonRequest.has(EmailUtils.FIELD_COMMAND)) {
            sendErrorResponse("Missing command field");
            return;
        }

        String command = jsonRequest.get(EmailUtils.FIELD_COMMAND).getAsString();

        if (command.equals(EmailUtils.LOGIN)) {
            handleLogin(jsonRequest);
            return;
        } else if (command.equals(EmailUtils.REGISTER)) {
            handleRegister(jsonRequest);
            return;
        } else if (command.equals(EmailUtils.EXIT)) {
            handleExit();
            return;
        }

        if (authenticatedUser == null) {
            sendErrorResponse("Authentication required");
            return;
        }

        switch (command) {
            case EmailUtils.LOGOUT:
                handleLogout();
                break;
            case EmailUtils.SEND:
                handleSendEmail(jsonRequest);
                break;
            case EmailUtils.LIST_INBOX:
                handleListInbox();
                break;
            case EmailUtils.SEARCH_INBOX:
                handleSearchInbox(jsonRequest);
                break;
            case EmailUtils.LIST_SENT:
                handleListSent();
                break;
            case EmailUtils.SEARCH_SENT:
                handleSearchSent(jsonRequest);
                break;
            case EmailUtils.READ:
                handleReadEmail(jsonRequest);
                break;
            default:
                sendErrorResponse("Unknown command: " + command);
        }
    }

    /**
     * Handles the login command.
     *
     * @param jsonRequest The JSON request object containing the login details.
     */
    private void handleLogin(JsonObject jsonRequest) {
        //check for username and password not given
        if (!jsonRequest.has(EmailUtils.FIELD_USERNAME) || !jsonRequest.has("password")) {
            sendErrorResponse("Missing username or password");
            return;
        }

        String username = jsonRequest.get(EmailUtils.FIELD_USERNAME).getAsString();
        String password = jsonRequest.get("password").getAsString();

        //username and password authenticated
        if (userManager.authenticate(username, password)) {
            authenticatedUser = username;

            //put into active clients map
            activeClients.put(username, this);

            //creates json object to be sent to client
            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_LOGIN_SUCCESS);
            response.addProperty(EmailUtils.FIELD_USERNAME, username);
            sendJsonResponse(response);
            LOGGER.info("User logged in: " + username);
        } else {
            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_LOGIN_FAILURE);
            sendJsonResponse(response);
            LOGGER.info("Login failed for: " + username);
        }
    }

    /**
     * Handles the registration command.
     *
     * @param jsonRequest The JSON request object containing the registration details.
     */
    private void handleRegister(JsonObject jsonRequest) {
        if (!jsonRequest.has(EmailUtils.FIELD_USERNAME) || !jsonRequest.has("password")) {
            sendErrorResponse("Missing username or password");
            return;
        }

        //gets username and password from json request and converts to string
        String username = jsonRequest.get(EmailUtils.FIELD_USERNAME).getAsString();
        String password = jsonRequest.get("password").getAsString();

        //hashes password
        String passwordHash = PasswordHasher.hashPassword(password);

        //authenticates user
        if (userManager.register(username, passwordHash)) {
            authenticatedUser = username;

            activeClients.put(username, this);

            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_REGISTERED);
            response.addProperty(EmailUtils.FIELD_USERNAME, username);
            sendJsonResponse(response);
            LOGGER.info("User registered: " + username);
        } else {
            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_REGISTER_FAILURE);
            sendJsonResponse(response);
            LOGGER.info("Registration failed for: " + username);
        }
    }

    /**
     * Handles the logout command.
     */
    private void handleLogout() {
        if (authenticatedUser != null) {
            activeClients.remove(authenticatedUser);
            String logoutUser = authenticatedUser;
            authenticatedUser = null;

            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_LOGOUT_SUCCESS);
            sendJsonResponse(response);
            LOGGER.info("User logged out: " + logoutUser);
        } else {
            sendErrorResponse("Not logged in");
        }
    }

    /**
     * Handles the exit command.
     */
    private void handleExit() {
        JsonObject response = new JsonObject();
        response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_GOODBYE);
        sendJsonResponse(response);

        sessionActive = false;
        LOGGER.info("Client exiting: " + (authenticatedUser != null ? authenticatedUser : "unauthenticated"));
    }

    /**
     * Handles the send email command.
     *
     * @param jsonRequest The JSON request object containing the email details.
     */
    private void handleSendEmail(JsonObject jsonRequest) {
        try {
            // Validate that all required fields (recipient, subject, body) are present in the request
            if (!jsonRequest.has(EmailUtils.FIELD_RECIPIENT) ||
                    !jsonRequest.has(EmailUtils.FIELD_SUBJECT) ||
                    !jsonRequest.has(EmailUtils.FIELD_BODY)) {
                sendErrorResponse("Missing recipient, subject, or body");
                return;
            }

            //gets email details from json request
            String recipientStr = jsonRequest.get(EmailUtils.FIELD_RECIPIENT).getAsString();
            String subject = jsonRequest.get(EmailUtils.FIELD_SUBJECT).getAsString();
            String body = jsonRequest.get(EmailUtils.FIELD_BODY).getAsString();

            //parses recipients string into list of email addresses
            List<String> recipients = Arrays.asList(recipientStr.split("\\s*,\\s*"));

            // Attempt to send the email using the EmailManager
            LOGGER.info("Processing send email request from " + authenticatedUser + " to " + String.join(", ", recipients));
            Email email = emailManager.sendEmail(authenticatedUser, recipients, subject, body);

            if (email != null) {
                JsonObject response = new JsonObject();
                response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_SENT);
                sendJsonResponse(response);
                LOGGER.info("Email sent from " + authenticatedUser + " to " + String.join(", ", recipients));
            } else {
                JsonObject response = new JsonObject();
                response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_SEND_FAILURE);
                response.addProperty(EmailUtils.FIELD_ERROR, "One or more recipients not found");
                sendJsonResponse(response);
                LOGGER.warning("Failed to send email: one or more recipients not found");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in handleSendEmail", e);
            sendErrorResponse("Internal server error: " + e.getMessage());
        }
    }


    /**
     * Handles the list inbox command.
     */
    private void handleListInbox() {
        List<Email> inboxEmails = emailManager.listInbox(authenticatedUser);

        if (inboxEmails.isEmpty()) {
            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_INBOX_EMPTY);
            sendJsonResponse(response);
        } else {
            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_INBOX);

            JsonArray emailsArray = new JsonArray();
            Map<Integer, Email> userEmailsWithIds = emailManager.getEmailIdsForUser(authenticatedUser);

            for (Map.Entry<Integer, Email> entry : userEmailsWithIds.entrySet()) {
                Email email = entry.getValue();
                if (email.getRecipients().contains(authenticatedUser)) {
                    JsonObject emailNode = new JsonObject();
                    emailNode.addProperty("id", entry.getKey());
                    emailNode.addProperty(EmailUtils.FIELD_SENDER, email.getSender());
                    emailNode.addProperty(EmailUtils.FIELD_SUBJECT, email.getSubject());
                    emailNode.addProperty(EmailUtils.FIELD_TIMESTAMP, email.getTimestamp().format(DATE_FORMATTER));
                    emailsArray.add(emailNode);
                }
            }

            response.add(EmailUtils.FIELD_EMAILS, emailsArray);
            sendJsonResponse(response);
        }
    }

    /**
     * Handles the search inbox command.
     *
     * @param jsonRequest The JSON request object containing the search term.
     */
    private void handleSearchInbox(JsonObject jsonRequest) {
        if (!jsonRequest.has("term")) {
            sendErrorResponse("Missing search term");
            return;
        }

        String searchTerm = jsonRequest.get("term").getAsString();
        List<Email> searchResults = emailManager.searchInbox(authenticatedUser, searchTerm);

        if (searchResults.isEmpty()) {
            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_NO_MATCHES);
            sendJsonResponse(response);
        } else {
            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_SEARCH_RESULTS);

            JsonArray emailsArray = new JsonArray();
            Map<Integer, Email> userEmailsWithIds = emailManager.getEmailIdsForUser(authenticatedUser);

            for (Map.Entry<Integer, Email> entry : userEmailsWithIds.entrySet()) {
                Email email = entry.getValue();
                if (searchResults.contains(email) && email.getRecipients().contains(authenticatedUser)) {
                    JsonObject emailNode = new JsonObject();
                    emailNode.addProperty("id", entry.getKey());
                    emailNode.addProperty(EmailUtils.FIELD_SENDER, email.getSender());
                    emailNode.addProperty(EmailUtils.FIELD_SUBJECT, email.getSubject());
                    emailNode.addProperty(EmailUtils.FIELD_TIMESTAMP, email.getTimestamp().format(DATE_FORMATTER));
                    emailsArray.add(emailNode);
                }
            }

            response.add(EmailUtils.FIELD_EMAILS, emailsArray);
            sendJsonResponse(response);
        }
    }

    private void handleListSent() {
        List<Email> sentEmails = emailManager.listSent(authenticatedUser);

        if (sentEmails.isEmpty()) {
            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_SENT_EMPTY);
            sendJsonResponse(response);
        } else {
            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_SENT_LIST);

            JsonArray emailsArray = new JsonArray();
            Map<Integer, Email> userEmailsWithIds = emailManager.getEmailIdsForUser(authenticatedUser);

            for (Map.Entry<Integer, Email> entry : userEmailsWithIds.entrySet()) {
                Email email = entry.getValue();
                if (email.getSender().equals(authenticatedUser)) {
                    JsonObject emailNode = new JsonObject();
                    emailNode.addProperty("id", entry.getKey());
                    emailNode.addProperty(EmailUtils.FIELD_RECIPIENT, String.join(", ", email.getRecipients()));
                    emailNode.addProperty(EmailUtils.FIELD_SUBJECT, email.getSubject());
                    emailNode.addProperty(EmailUtils.FIELD_TIMESTAMP, email.getTimestamp().format(DATE_FORMATTER));

                    if (email.getRecipients().size() > 0) {
                        JsonObject viewedStatusNode = new JsonObject();
                        for (String recipient : email.getRecipients()) {
                            viewedStatusNode.addProperty(recipient,
                                    emailManager.hasRecipientViewedEmail(email, recipient));
                        }
                        emailNode.add("viewedByRecipients", viewedStatusNode);
                    }

                    emailsArray.add(emailNode);
                }
            }

            response.add(EmailUtils.FIELD_EMAILS, emailsArray);
            sendJsonResponse(response);
        }
    }

    private void handleSearchSent(JsonObject jsonRequest) {
        if (!jsonRequest.has("term")) {
            sendErrorResponse("Missing search term");
            return;
        }

        String searchTerm = jsonRequest.get("term").getAsString();
        List<Email> searchResults = emailManager.searchSent(authenticatedUser, searchTerm);

        if (searchResults.isEmpty()) {
            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_NO_MATCHES);
            sendJsonResponse(response);
        } else {
            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_SEARCH_SENT_RESULTS);

            JsonArray emailsArray = new JsonArray();
            Map<Integer, Email> userEmailsWithIds = emailManager.getEmailIdsForUser(authenticatedUser);

            for (Map.Entry<Integer, Email> entry : userEmailsWithIds.entrySet()) {
                Email email = entry.getValue();
                if (searchResults.contains(email) && email.getSender().equals(authenticatedUser)) {
                    JsonObject emailNode = new JsonObject();
                    emailNode.addProperty("id", entry.getKey());
                    emailNode.addProperty(EmailUtils.FIELD_RECIPIENT, String.join(", ", email.getRecipients()));
                    emailNode.addProperty(EmailUtils.FIELD_SUBJECT, email.getSubject());
                    emailNode.addProperty(EmailUtils.FIELD_TIMESTAMP, email.getTimestamp().format(DATE_FORMATTER));
                    emailsArray.add(emailNode);
                }
            }

            response.add(EmailUtils.FIELD_EMAILS, emailsArray);
            sendJsonResponse(response);
        }
    }

    private void handleReadEmail(JsonObject jsonRequest) {
        if (!jsonRequest.has("id")) {
            sendErrorResponse("Missing email ID");
            return;
        }

        int emailId = jsonRequest.get("id").getAsInt();
        Optional<Email> optionalEmail = emailManager.getEmailById(emailId, authenticatedUser);

        if (optionalEmail.isPresent()) {
            Email email = optionalEmail.get();

            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_EMAIL_CONTENT);

            JsonObject emailNode = new JsonObject();
            emailNode.addProperty(EmailUtils.FIELD_SENDER, email.getSender());
            emailNode.addProperty(EmailUtils.FIELD_RECIPIENT, String.join(", ", email.getRecipients()));
            emailNode.addProperty(EmailUtils.FIELD_SUBJECT, email.getSubject());
            emailNode.addProperty(EmailUtils.FIELD_BODY, email.getBody());
            emailNode.addProperty(EmailUtils.FIELD_TIMESTAMP, email.getTimestamp().format(DATE_FORMATTER));

            if (email.getRecipients().size() > 1 && email.getSender().equals(authenticatedUser)) {
                Map<String, Boolean> viewedStatus = emailManager.getViewStatusForAllRecipients(email);
                JsonObject viewedStatusNode = new JsonObject();

                for (Map.Entry<String, Boolean> entry : viewedStatus.entrySet()) {
                    viewedStatusNode.addProperty(entry.getKey(), entry.getValue());
                }

                emailNode.add("viewedByRecipients", viewedStatusNode);
            }

            response.add(EmailUtils.FIELD_EMAIL, emailNode);
            sendJsonResponse(response);
        } else {
            JsonObject response = new JsonObject();
            response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_EMAIL_NOT_FOUND);
            sendJsonResponse(response);
        }
    }

    private void sendErrorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty(EmailUtils.FIELD_STATUS, EmailUtils.STATUS_ERROR);
        response.addProperty("message", message);
        sendJsonResponse(response);
    }

    private void sendJsonResponse(JsonObject jsonResponse) {
        String response = gson.toJson(jsonResponse);
        networkLayer.send(response);
        LOGGER.info("Sent response: " + response);
    }

    private void cleanup() {
        if (authenticatedUser != null) {
            activeClients.remove(authenticatedUser);
            LOGGER.info("Removed user from active clients: " + authenticatedUser);
        }

        try {
            if (networkLayer != null) {
                networkLayer.disconnect();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error disconnecting network layer", e);
        }

        LOGGER.info("Client handler cleaned up");
    }

}