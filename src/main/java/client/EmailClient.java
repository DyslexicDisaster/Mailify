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
            network = new NetworkLayerJSON(EmailUtils.HOSTNAME, EmailUtils.PORT);
            network.connect();
            showMainMenu();
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    private static void showMainMenu() {
        while (true) {
            System.out.println("\n=== Email Client ===");
            if (loggedInUser != null) {
                System.out.println("Logged in as: " + loggedInUser);
                System.out.println("1. Send Email");
                System.out.println("2. List Inbox");
                System.out.println("3. List Sent");
                System.out.println("4. Search Inbox");
                System.out.println("5. Search Sent");
                System.out.println("6. Read Email");
                System.out.println("7. Logout");
            } else {
                System.out.println("1. Login");
                System.out.println("2. Register");
            }
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            String choice = input.nextLine();
            handleChoice(choice);
        }
    }

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

    private static void handleAuthenticatedChoice(String choice) throws IOException {
        switch (choice) {
            case "1":
                sendEmail();
                break;
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
                handleEmailOperations(choice);
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

    private static void handleAuthCommand(String command) throws IOException {
        System.out.print("Username: ");
        String username = input.nextLine();
        System.out.print("Password: ");
        String password = input.nextLine();

        JsonObject request = new JsonObject();
        request.addProperty(EmailUtils.FIELD_COMMAND, command);
        request.addProperty(EmailUtils.FIELD_USERNAME, username);
        request.addProperty(EmailUtils.FIELD_PASSWORD, password);

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

    private static void sendEmail() throws IOException {
        System.out.print("Recipient: ");
        String recipient = input.nextLine();
        System.out.print("Subject: ");
        String subject = input.nextLine();
        System.out.print("Body: ");
        String body = input.nextLine();

        JsonObject request = new JsonObject();
        request.addProperty(EmailUtils.FIELD_COMMAND, EmailUtils.SEND);
        request.addProperty(EmailUtils.FIELD_RECIPIENT, recipient);
        request.addProperty(EmailUtils.FIELD_SUBJECT, subject);
        request.addProperty(EmailUtils.FIELD_BODY, body);

        sendAndHandleResponse(request, response -> {
            String status = response.get(EmailUtils.FIELD_STATUS).getAsString();
            if (status.equals(EmailUtils.STATUS_SENT)) {
                System.out.println("Email sent successfully!");
            } else {
                System.out.println("Error: " + response.get(EmailUtils.FIELD_ERROR).getAsString());
            }
        });
    }

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

    private static void handleEmailResponse(JsonObject response) {
        String status = response.get(EmailUtils.FIELD_STATUS).getAsString();

        if (response.has(EmailUtils.FIELD_EMAILS)) {
            JsonArray emails = response.getAsJsonArray(EmailUtils.FIELD_EMAILS);
            System.out.println(gson.toJson(emails));
        } else if (response.has(EmailUtils.FIELD_EMAIL)) {
            JsonObject email = response.getAsJsonObject(EmailUtils.FIELD_EMAIL);
            System.out.println(gson.toJson(email));
        } else {
            System.out.println(status);
        }
    }

    private static void logout() throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty(EmailUtils.FIELD_COMMAND, EmailUtils.LOGOUT);
        sendAndHandleResponse(request, response -> {
            loggedInUser = null;
            System.out.println("Logged out successfully");
        });
    }

    private static void exitClient() throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty(EmailUtils.FIELD_COMMAND, EmailUtils.EXIT);
        network.send(gson.toJson(request));
        System.out.println("Goodbye!");
        System.exit(0);
    }

    private static void sendAndHandleResponse(JsonObject request, ResponseHandler handler) throws IOException {
        String jsonRequest = gson.toJson(request);
        network.send(jsonRequest);
        String responseStr = network.receive();
        JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();
        handler.handle(response);
    }

    interface ResponseHandler {
        void handle(JsonObject response);
    }
}