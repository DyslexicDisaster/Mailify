package model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages email creation, storage, retrieval, and search operations in memory.
 * Uses a thread-safe map for email storage and an atomic counter for unique IDs.
 */
public class EmailManager implements IEmailManager {
    /**
     * Thread-safe map of email IDs to Email objects.
     */
    private final Map<Integer, Email> emailsById = new ConcurrentHashMap<>();

    /**
     * Generates unique email IDs in a thread-safe manner.
     */
    private final AtomicInteger nextEmailId = new AtomicInteger(1);

    /**
     * Reference to the user manager for validating users and updating inbox/sent lists.
     */
    private final UserManager userManager;

    /**
     * Constructs an EmailManager with the given UserManager and bootstraps sample emails.
     *
     * @param userManager the UserManager for validating senders/recipients and updating user mailboxes
     */
    public EmailManager(UserManager userManager) {
        this.userManager = userManager;
        bootstrapEmails();
    }

    /**
     * Populates the system with sample emails for testing between bootstrap users.
     */
    private void bootstrapEmails() {
        sendEmail("testUser1", "testUser2", "Welcome to the email system",
                "Hello testUser2, Email 1 cool email");

        sendEmail("testUser2", "testUser1", "RE: Welcome to the email system",
                "Hi testUser1 what are you upto");

        List<String> recipients = Arrays.asList("testUser2", "testUser3");
        sendEmail("testUser1", recipients, "Team meeting tomorrow",
                "Let's meet tomorrow at 2pm to discuss the project.");
    }

    /**
     * Sends an email from a sender to multiple recipients.
     * Validates existence of sender and all recipients before sending.
     *
     * @param sender     username of the email sender
     * @param recipients list of recipient usernames
     * @param subject    subject line of the email
     * @param body       body text of the email
     * @return the Email object if sent successfully, or null if any user validation fails
     */
    @Override
    public Email sendEmail(String sender, List<String> recipients, String subject, String body) {
        if (!userManager.userExists(sender)) {
            return null;
        }
        for (String recipient : recipients) {
            if (!userManager.userExists(recipient)) {
                return null;
            }
        }

        Email email = new Email();
        email.setSender(sender);
        email.setRecipients(new ArrayList<>(recipients));
        email.setSubject(subject);
        email.setBody(body);
        email.setTimestamp(LocalDateTime.now());
        email.setViewed(false);

        int emailId = nextEmailId.getAndIncrement();
        emailsById.put(emailId, email);

        // Add to sender's sent list
        User senderUser = userManager.getUserByUsername(sender);
        senderUser.getSent().add(email);

        // Add to each recipient's inbox
        for (String recipient : recipients) {
            User recipientUser = userManager.getUserByUsername(recipient);
            recipientUser.getInbox().add(email);
        }

        return email;
    }

    /**
     * Sends an email from a sender to a single recipient.
     *
     * @param sender    username of the email sender
     * @param recipient username of the email recipient
     * @param subject   subject line of the email
     * @param body      body text of the email
     * @return the Email object if sent successfully, or null if any user validation fails
     */
    @Override
    public Email sendEmail(String sender, String recipient, String subject, String body) {
        return sendEmail(sender, Collections.singletonList(recipient), subject, body);
    }

    /**
     * Lists all received emails for the given user.
     *
     * @param username the username whose inbox to list
     * @return a List of Email objects in the user's inbox, or empty list if user not found
     */
    @Override
    public List<Email> listInbox(String username) {
        User user = userManager.getUserByUsername(username);
        return (user == null) ? Collections.emptyList() : new ArrayList<>(user.getInbox());
    }

    /**
     * Searches for emails in a user's inbox matching the search term in sender, subject, or body.
     *
     * @param username the username whose inbox to search
     * @param term     the search keyword
     * @return a List of matching Email objects, or empty list if user not found
     */
    @Override
    public List<Email> searchInbox(String username, String term) {
        User user = userManager.getUserByUsername(username);
        if (user == null) {
            return Collections.emptyList();
        }
        String lowercaseTerm = term.toLowerCase();
        return user.getInbox().stream()
                .filter(email -> email.getSender().toLowerCase().contains(lowercaseTerm)
                        || email.getSubject().toLowerCase().contains(lowercaseTerm)
                        || email.getBody().toLowerCase().contains(lowercaseTerm))
                .collect(Collectors.toList());
    }

    /**
     * Lists all sent emails for the given user.
     *
     * @param username the username whose sent mailbox to list
     * @return a List of Email objects in the user's sent folder, or empty list if user not found
     */
    @Override
    public List<Email> listSent(String username) {
        User user = userManager.getUserByUsername(username);
        return (user == null) ? Collections.emptyList() : new ArrayList<>(user.getSent());
    }

    /**
     * Searches for emails in a user's sent folder matching the search term in recipient, subject, or body.
     *
     * @param username the username whose sent folder to search
     * @param term     the search keyword
     * @return a List of matching Email objects, or empty list if user not found
     */
    @Override
    public List<Email> searchSent(String username, String term) {
        User user = userManager.getUserByUsername(username);
        if (user == null) {
            return Collections.emptyList();
        }
        String lowercaseTerm = term.toLowerCase();
        return user.getSent().stream()
                .filter(email -> email.getRecipients().stream()
                        .anyMatch(r -> r.toLowerCase().contains(lowercaseTerm))
                        || email.getSubject().toLowerCase().contains(lowercaseTerm)
                        || email.getBody().toLowerCase().contains(lowercaseTerm))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves an email by ID if the specified user is the sender or a recipient.
     * Marks the email as viewed for recipients.
     *
     * @param id       the unique email ID
     * @param username the username requesting the email
     * @return an Optional containing the Email if found and accessible, otherwise empty
     */
    @Override
    public Optional<Email> getEmailById(int id, String username) {
        Email email = emailsById.get(id);
        if (email == null) {
            return Optional.empty();
        }
        boolean isSender = email.getSender().equals(username);
        boolean isRecipient = email.getRecipients().contains(username);
        if (isSender || isRecipient) {
            if (isRecipient) {
                User user = userManager.getUserByUsername(username);
                user.markEmailAsViewed(email);
            }
            return Optional.of(email);
        }
        return Optional.empty();
    }

    /**
     * Checks if a specific recipient has viewed the given email.
     *
     * @param email             the Email to check
     * @param recipientUsername the recipient's username
     * @return true if viewed, false otherwise or if user not found
     */
    public boolean hasRecipientViewedEmail(Email email, String recipientUsername) {
        User recipient = userManager.getUserByUsername(recipientUsername);
        return recipient != null && recipient.hasViewedEmail(email);
    }

    /**
     * Retrieves the view status (read/unread) for all recipients of an email.
     *
     * @param email the Email whose recipients' statuses to retrieve
     * @return a map of recipient usernames to their view status
     */
    public Map<String, Boolean> getViewStatusForAllRecipients(Email email) {
        Map<String, Boolean> viewStatus = new HashMap<>();
        for (String recipient : email.getRecipients()) {
            viewStatus.put(recipient, hasRecipientViewedEmail(email, recipient));
        }
        return viewStatus;
    }

    /**
     * Retrieves a map of email IDs to Email objects for all messages where the
     * specified user is either sender or recipient.
     *
     * @param username the username whose emails to retrieve
     * @return a map of email IDs to Email objects
     */
    public Map<Integer, Email> getEmailIdsForUser(String username) {
        Map<Integer, Email> userEmails = new HashMap<>();
        for (Map.Entry<Integer, Email> entry : emailsById.entrySet()) {
            Email email = entry.getValue();
            if (email.getSender().equals(username) || email.getRecipients().contains(username)) {
                userEmails.put(entry.getKey(), email);
            }
        }
        return userEmails;
    }
}