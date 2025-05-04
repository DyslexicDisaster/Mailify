package model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EmailManager implements IEmailManager {
    private final Map<Integer, Email> emailsById = new ConcurrentHashMap<>();
    private final AtomicInteger nextEmailId = new AtomicInteger(1);
    // We use atomic integer to ensure each email has a unique ID by atomizing and incrementing we prevent race conditions
    private final UserManager userManager;

    public EmailManager(UserManager userManager) {
        this.userManager = userManager;
        bootstrapEmails();
    }

    private void bootstrapEmails() {
        // Send some sample emails between test users
        sendEmail("testUser1", "testUser2", "Welcome to the email system",
                "Hello testUser2, Email 1 cool email");

        sendEmail("testUser2", "testUser1", "RE: Welcome to the email system",
                "Hi testUser1 what are you upto");

        List<String> recipients = new ArrayList<>();
        recipients.add("testUser2");
        recipients.add("testUser3");
        sendEmail("testUser1", recipients, "Team meeting tomorrow",
                "Let's meet tomorrow at 2pm to discuss the project.");
    }

    @Override
    public Email sendEmail(String sender, List<String> recipients, String subject, String body) {
        if (!userManager.userExists(sender)) {
            return null;
        }
        boolean allRecipientsExist = true;
        for (String recipient : recipients) {
            if (!userManager.userExists(recipient)) {
                allRecipientsExist = false;
                break;
            }
        }

        if (!allRecipientsExist) {
            return null;
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

        User senderUser = userManager.getUserByUsername(sender);
        senderUser.getSent().add(email);

        for (String recipient : recipients) {
            User recipientUser = userManager.getUserByUsername(recipient);
            recipientUser.getInbox().add(email);
        }

        return email;
    }

    @Override
    public Email sendEmail(String sender, String recipient, String subject, String body) {
        List<String> recipients = new ArrayList<>();
        recipients.add(recipient);
        return sendEmail(sender, recipients, subject, body);
    }


    @Override
    public List<Email> listInbox(String username) {
        User user = userManager.getUserByUsername(username);
        if (user == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(user.getInbox());
    }

    @Override
    public List<Email> searchInbox(String username, String term) {
        User user = userManager.getUserByUsername(username);
        if (user == null) {
            return Collections.emptyList();
        }

        String lowercaseTerm = term.toLowerCase();
        return user.getInbox().stream()
                .filter(email ->
                        email.getSender().toLowerCase().contains(lowercaseTerm) ||
                                email.getSubject().toLowerCase().contains(lowercaseTerm) ||
                                email.getBody().toLowerCase().contains(lowercaseTerm))
                .collect(Collectors.toList());
    }

    @Override
    public List<Email> listSent(String username) {
        User user = userManager.getUserByUsername(username);
        if (user == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(user.getSent());
    }

    @Override
    public List<Email> searchSent(String username, String term) {
        User user = userManager.getUserByUsername(username);
        if (user == null) {
            return Collections.emptyList();
        }

        String lowercaseTerm = term.toLowerCase();
        return user.getSent().stream()
                .filter(email ->
                        email.getRecipients().stream().anyMatch(r -> r.toLowerCase().contains(lowercaseTerm)) ||
                                email.getSubject().toLowerCase().contains(lowercaseTerm) ||
                                email.getBody().toLowerCase().contains(lowercaseTerm))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Email> getEmailById(int id, String username) {
        Email email = emailsById.get(id);
        if (email == null) {
            return Optional.empty();
        }

        if (email.getSender().equals(username) || email.getRecipients().contains(username)) {

            if (email.getRecipients().contains(username)) {
                User user = userManager.getUserByUsername(username);
                user.markEmailAsViewed(email);
            }

            return Optional.of(email);
        }

        return Optional.empty();
    }

    public boolean hasRecipientViewedEmail(Email email, String recipientUsername) {
        User recipient = userManager.getUserByUsername(recipientUsername);
        if (recipient == null) {
            return false;
        }
        return recipient.hasViewedEmail(email);
    }


    public Map<String, Boolean> getViewStatusForAllRecipients(Email email) {
        Map<String, Boolean> viewStatus = new HashMap<>();
        for (String recipient : email.getRecipients()) {
            viewStatus.put(recipient, hasRecipientViewedEmail(email, recipient));
        }
        return viewStatus;
    }

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