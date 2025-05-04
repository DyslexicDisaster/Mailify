package client;

import com.google.gson.*;
import network.NetworkLayerJSON;
import utils.EmailUtils;

import java.io.IOException;
import java.util.Scanner;

public class EmailClient {
    private static final Gson gson = new Gson();
    private static final Scanner input = new Scanner(System.in);
    private static NetworkLayerJSON network;
    private static String loggedInUser = null;

    public static void main(String[] args) {
        try {
            // Establish connection to the server
            network = new NetworkLayerJSON(EmailUtils.HOSTNAME, EmailUtils.PORT);
            network.connect();
            // Enter the main interactive menu loop
            showMainMenu();
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    /**
     * Displays the main menu, routing to login/register or email operations
     * based on whether a user is currently logged in.
     */
    private static void showMainMenu() {
        while (true) {
            System.out.println("\n=== Email Client ===");
            if (loggedInUser != null) {
                // Menu for authenticated users
                System.out.println("Logged in as: " + loggedInUser);
                System.out.println("1. Send Email");
                System.out.println("2. List Inbox");
                System.out.println("3. List Sent");
                System.out.println("4. Search Inbox");
                System.out.println("5. Search Sent");
                System.out.println("6. Read Email");
                System.out.println("7. Logout");
            } else {
                // Menu for unauthenticated users
                System.out.println("1. Login");
                System.out.println("2. Register");
            }
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            String choice = input.nextLine();
            handleChoice(choice);
        }
    }

    /**
     * Delegates user menu choices to the appropriate handler.
     */
    private static void handleChoice(String choice) {
        try {
            if (loggedInUser == null) {
                handleUnauthenticatedChoice(choice);
            } else {
                handleAuthenticatedChoice(choice);
            }
        } catch (IOException e) {
            System.out.println("Network error: " + e.getMessage());
        }
    }

    /**
     * Handles menu options when not logged in (login, register, exit).
     */
    private static void handleUnauthenticatedChoice(String choice) throws IOException {
        switch (choice) {
            case "1":
                handleAuthCommand(EmailUtils.LOGIN);
                break;
            case "2":
                handleAuthCommand(EmailUtils.REGISTER);
                break;
            case "0":
                exitClient();
                break;
            default:
                System.out.println("Invalid choice");
        }
    }

    /**
     * Handles menu options when logged in (send, list, search, read, logout, exit).
     */
    private static void handleAuthenticatedChoice(String choice) throws IOException {
        switch (choice) {
            case "1":
                sendEmail();
                break;
            case "2":
            case "3":
            case "4":
            case "5":
                handleEmailOperations(choice);
                break;
            case "6":
                readEmail();
                break;
            case "7":
                logout();
                break;
            case "0":
                exitClient();
                break;
            default:
                System.out.println("Invalid choice");
        }
    }

    /**
     * Shared handler for LOGIN and REGISTER commands.
     */
    private static void handleAuthCommand(String command) throws IOException {
        System.out.print("Username: ");
        String username = input.nextLine();
        System.out.print("Password: ");
        String password = input.nextLine();

        // Build JSON request
        JsonObject request = new JsonObject();
        request.addProperty(EmailUtils.FIELD_COMMAND, command);
        request.addProperty(EmailUtils.FIELD_USERNAME, username);
        request.addProperty(EmailUtils.FIELD_PASSWORD, password);

        // Send and process the response
        sendAndHandleResponse(request, response -> {
            String status = response.get(EmailUtils.FIELD_STATUS).getAsString();
            if (status.equals(EmailUtils.STATUS_LOGIN_SUCCESS)) {
                loggedInUser = username;
                System.out.println("Login successful!");
            } else if (status.equals(EmailUtils.STATUS_REGISTERED)) {
                System.out.println("Registration successful!");
            } else {
                System.out.println("Error: " + response.get(EmailUtils.FIELD_ERROR).getAsString());
            }
        });
    }

    /**
     * Prompts for email details and sends a SEND command.
     */
    private static void sendEmail() throws IOException {
        System.out.print("Recipient: ");
        String recipient = input.nextLine();
        System.out.print("Subject: ");
        String subject = input.nextLine();
        System.out.print("Body: ");
        String body = input.nextLine();

        // Build SEND request
        JsonObject request = new JsonObject();
        request.addProperty(EmailUtils.FIELD_COMMAND, EmailUtils.SEND);
        request.addProperty(EmailUtils.FIELD_RECIPIENT, recipient);
        request.addProperty(EmailUtils.FIELD_SUBJECT, subject);
        request.addProperty(EmailUtils.FIELD_BODY, body);

        // Send and process
        sendAndHandleResponse(request, response -> {
            String status = response.get(EmailUtils.FIELD_STATUS).getAsString();
            if (status.equals(EmailUtils.STATUS_SENT)) {
                System.out.println("Email sent successfully!");
            } else {
                System.out.println("Error: " + response.get(EmailUtils.FIELD_ERROR).getAsString());
            }
        });
    }

    /**
     * Handles list/search/read commands based on the user's menu choice.
     */
    private static void handleEmailOperations(String choice) throws IOException {
        JsonObject request = new JsonObject();
        String command = "";

        switch (choice) {
            case "2":
                command = EmailUtils.LIST_INBOX;
                break;
            case "3":
                command = EmailUtils.LIST_SENT;
                break;
            case "4":
                command = EmailUtils.SEARCH_INBOX;
                System.out.print("Search term: ");
                request.addProperty("term", input.nextLine());
                break;
            case "5":
                command = EmailUtils.SEARCH_SENT;
                System.out.print("Search term: ");
                request.addProperty("term", input.nextLine());
                break;
            case "6":
                command = EmailUtils.READ;
                System.out.print("Email ID: ");
                request.addProperty("id", input.nextLine());
                break;
        }

        request.addProperty(EmailUtils.FIELD_COMMAND, command);
        sendAndHandleResponse(request, EmailClient::handleEmailResponse);
    }

    /**
     * Prints out email lists or content based on the server response.
     */
    private static void handleEmailResponse(JsonObject response) {
        String status = response.get(EmailUtils.FIELD_STATUS).getAsString();

        if (response.has(EmailUtils.FIELD_EMAILS)) {
            // Display list of emails
            JsonArray emails = response.getAsJsonArray(EmailUtils.FIELD_EMAILS);
            System.out.println("\n=== Email List ===");
            for (JsonElement emailElement : emails) {
                JsonObject emailObj = emailElement.getAsJsonObject();
                System.out.println("ID: " + emailObj.get("id").getAsString());
                if (emailObj.has(EmailUtils.FIELD_SENDER)) {
                    System.out.println("From: " + emailObj.get(EmailUtils.FIELD_SENDER).getAsString());
                }
                if (emailObj.has(EmailUtils.FIELD_RECIPIENT)) {
                    System.out.println("To: " + emailObj.get(EmailUtils.FIELD_RECIPIENT).getAsString());
                }
                System.out.println("Subject: " + emailObj.get(EmailUtils.FIELD_SUBJECT).getAsString());
                System.out.println("Date: " + emailObj.get(EmailUtils.FIELD_TIMESTAMP).getAsString());
                System.out.println("---------------------");
            }
        } else if (response.has(EmailUtils.FIELD_EMAIL)) {
            // Display single email content
            JsonObject email = response.getAsJsonObject(EmailUtils.FIELD_EMAIL);
            System.out.println("\n=== Email Content ===");
            System.out.println("From: " + email.get(EmailUtils.FIELD_SENDER).getAsString());
            System.out.println("To: " + email.get(EmailUtils.FIELD_RECIPIENT).getAsString());
            System.out.println("Subject: " + email.get(EmailUtils.FIELD_SUBJECT).getAsString());
            System.out.println("Date: " + email.get(EmailUtils.FIELD_TIMESTAMP).getAsString());
            System.out.println("\n" + email.get(EmailUtils.FIELD_BODY).getAsString());
        } else {
            // Fallback: just print the status
            System.out.println(status);
        }
    }

    /**
     * Prompts user for an email ID and sends a READ command.
     */
    private static void readEmail() throws IOException {
        System.out.print("Enter email ID to read: ");
        String emailId = input.nextLine();

        // Validate numeric ID
        try {
            Integer.parseInt(emailId);
        } catch (NumberFormatException e) {
            System.out.println("Error: Email ID must be a number");
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty(EmailUtils.FIELD_COMMAND, EmailUtils.READ);
        request.addProperty("id", emailId);

        sendAndHandleResponse(request, EmailClient::handleEmailResponse);
    }

    /**
     * Sends a LOGOUT command and clears session state.
     */
    private static void logout() throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty(EmailUtils.FIELD_COMMAND, EmailUtils.LOGOUT);
        sendAndHandleResponse(request, response -> {
            loggedInUser = null;
            System.out.println("Logged out successfully");
        });
    }

    /**
     * Sends an EXIT command and terminates the application.
     */
    private static void exitClient() throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty(EmailUtils.FIELD_COMMAND, EmailUtils.EXIT);
        network.send(gson.toJson(request));
        System.out.println("Goodbye!");
        System.exit(0);
    }

    /**
     * Sends a JSON request, waits for the response, and dispatches it
     * to the given handler.
     */
    private static void sendAndHandleResponse(JsonObject request, ResponseHandler handler) throws IOException {
        String jsonRequest = gson.toJson(request);
        network.send(jsonRequest);
        String responseStr = network.receive();
        JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();
        handler.handle(response);
    }

    // Functional interface for handling JSON responses
    interface ResponseHandler {
        void handle(JsonObject response);
    }
}
