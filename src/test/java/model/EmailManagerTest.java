package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class EmailManagerTest {
    private UserManager userManager;
    private EmailManager emailManager;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        emailManager = new EmailManager(userManager);
    }

    @Test
    void testBootstrapEmails() {
        // Test the bootstrapped emails exist
        List<Email> user1Inbox = emailManager.listInbox("testUser1");
        List<Email> user2Inbox = emailManager.listInbox("testUser2");

        assertFalse(user1Inbox.isEmpty());
        assertFalse(user2Inbox.isEmpty());

        // Check that testUser1 has sent emails
        List<Email> user1Sent = emailManager.listSent("testUser1");
        assertFalse(user1Sent.isEmpty());

        // Verify some email content
        boolean foundTeamMeetingEmail = false;
        for (Email email : user1Sent) {
            if (email.getSubject().contains("Team meeting")) {
                foundTeamMeetingEmail = true;
                assertTrue(email.getRecipients().contains("testUser2"));
                assertTrue(email.getRecipients().contains("testUser3"));
            }
        }
        assertTrue(foundTeamMeetingEmail, "Should find the team meeting email");
    }

    @Test
    void testSendEmail() {
        // Test sending an email to a single recipient
        String sender = "testUser1";
        String recipient = "testUser2";
        String subject = "Test Single Recipient";
        String body = "This is a test email body";

        Email email = emailManager.sendEmail(sender, recipient, subject, body);

        assertNotNull(email);
        assertEquals(sender, email.getSender());
        assertEquals(1, email.getRecipients().size());
        assertEquals(recipient, email.getRecipients().get(0));
        assertEquals(subject, email.getSubject());
        assertEquals(body, email.getBody());
        assertNotNull(email.getTimestamp());

        // Verify it appears in sender's sent folder
        List<Email> sentEmails = emailManager.listSent(sender);
        boolean foundInSent = sentEmails.stream()
                .anyMatch(e -> e.getSubject().equals(subject) && e.getBody().equals(body));
        assertTrue(foundInSent, "Email should be in sender's sent folder");

        // Verify it appears in recipient's inbox
        List<Email> recipientInbox = emailManager.listInbox(recipient);
        boolean foundInInbox = recipientInbox.stream()
                .anyMatch(e -> e.getSubject().equals(subject) && e.getBody().equals(body));
        assertTrue(foundInInbox, "Email should be in recipient's inbox");
    }

    @Test
    void testSendEmailToMultipleRecipients() {
        // Test sending an email to multiple recipients
        String sender = "testUser1";
        List<String> recipients = Arrays.asList("testUser2", "testUser3");
        String subject = "Test Multiple Recipients";
        String body = "This is a test email to multiple recipients";

        Email email = emailManager.sendEmail(sender, recipients, subject, body);

        assertNotNull(email);
        assertEquals(sender, email.getSender());
        assertEquals(recipients.size(), email.getRecipients().size());
        assertTrue(email.getRecipients().containsAll(recipients));
        assertEquals(subject, email.getSubject());
        assertEquals(body, email.getBody());

        // Verify it appears in all recipients' inboxes
        for (String recipient : recipients) {
            List<Email> inbox = emailManager.listInbox(recipient);
            boolean found = inbox.stream()
                    .anyMatch(e -> e.getSubject().equals(subject) && e.getBody().equals(body));
            assertTrue(found, "Email should be in " + recipient + "'s inbox");
        }
    }

    @Test
    void testSendEmailWithInvalidUsers() {
        // Test sending with invalid sender
        Email email = emailManager.sendEmail("nonExistentUser", "testUser1", "Subject", "Body");
        assertNull(email, "Should not send email with invalid sender");

        // Test sending with invalid recipient
        email = emailManager.sendEmail("testUser1", "nonExistentUser", "Subject", "Body");
        assertNull(email, "Should not send email with invalid recipient");

        // Test sending with invalid recipients in a list
        List<String> recipients = Arrays.asList("testUser2", "nonExistentUser");
        email = emailManager.sendEmail("testUser1", recipients, "Subject", "Body");
        assertNull(email, "Should not send email with any invalid recipients");
    }

    @Test
    void testListInbox() {
        // Send a test email to ensure inbox has something
        emailManager.sendEmail("testUser1", "testUser2", "Inbox Test", "Testing inbox listing");

        // Get inbox for recipient
        List<Email> inbox = emailManager.listInbox("testUser2");

        assertFalse(inbox.isEmpty(), "Inbox should not be empty");

        // Verify the test email is in the inbox
        boolean found = inbox.stream()
                .anyMatch(e -> e.getSubject().equals("Inbox Test") && e.getSender().equals("testUser1"));
        assertTrue(found, "Test email should be in inbox");

        // Test inbox for non-existent user
        List<Email> nonExistentUserInbox = emailManager.listInbox("nonExistentUser");
        assertTrue(nonExistentUserInbox.isEmpty(), "Non-existent user should have empty inbox");
    }

    @Test
    void testSearchInbox() {
        // Send emails with specific content to search for
        emailManager.sendEmail("testUser1", "testUser2", "Search Test Alpha", "This is searchable content");
        emailManager.sendEmail("testUser1", "testUser2", "Another Test", "This contains alpha in the body");
        emailManager.sendEmail("testUser3", "testUser2", "Unrelated", "This should not match");

        // Search for "alpha" which should match two emails
        List<Email> searchResults = emailManager.searchInbox("testUser2", "alpha");

        assertEquals(2, searchResults.size(), "Should find two emails matching 'alpha'");

        // Verify the results contain the expected emails
        boolean foundSubject = false;
        boolean foundBody = false;

        for (Email email : searchResults) {
            if (email.getSubject().toLowerCase().contains("alpha")) {
                foundSubject = true;
            }
            if (email.getBody().toLowerCase().contains("alpha")) {
                foundBody = true;
            }
        }

        assertTrue(foundSubject, "Should find email with 'alpha' in subject");
        assertTrue(foundBody, "Should find email with 'alpha' in body");

        // Test search with term not present
        List<Email> emptyResults = emailManager.searchInbox("testUser2", "nonexistentterm");
        assertTrue(emptyResults.isEmpty(), "Search for non-existent term should return empty list");

        // Test search for non-existent user
        List<Email> nonExistentUserResults = emailManager.searchInbox("nonExistentUser", "alpha");
        assertTrue(nonExistentUserResults.isEmpty(), "Search for non-existent user should return empty list");
    }

    @Test
    void testListSent() {
        // Send a test email
        emailManager.sendEmail("testUser1", "testUser2", "Sent Test", "Testing sent listing");

        // Get sent emails for sender
        List<Email> sent = emailManager.listSent("testUser1");

        assertFalse(sent.isEmpty(), "Sent folder should not be empty");

        // Verify the test email is in the sent folder
        boolean found = sent.stream()
                .anyMatch(e -> e.getSubject().equals("Sent Test") && e.getRecipients().contains("testUser2"));
        assertTrue(found, "Test email should be in sent folder");

        // Test sent folder for non-existent user
        List<Email> nonExistentUserSent = emailManager.listSent("nonExistentUser");
        assertTrue(nonExistentUserSent.isEmpty(), "Non-existent user should have empty sent folder");
    }

    @Test
    void testSearchSent() {
        // Send emails with specific content to search for
        emailManager.sendEmail("testUser1", "testUser2", "Sent Search Test chicken", "This is sent search content");
        emailManager.sendEmail("testUser1", "testUser3", "Another Sent Test", "This contains chicken in the body");
        emailManager.sendEmail("testUser1", "testUser2", "Unrelated Sent", "This should not match");

        // Search for chicken which should match two emails
        List<Email> searchResults = emailManager.searchSent("testUser1", "chicken");

        assertEquals(2, searchResults.size(), "Should find two sent emails matching 'chicken'");

        // Verify the results contain the expected emails
        boolean foundSubject = false;
        boolean foundBody = false;

        for (Email email : searchResults) {
            if (email.getSubject().toLowerCase().contains("chicken")) {
                foundSubject = true;
            }
            if (email.getBody().toLowerCase().contains("chicken")) {
                foundBody = true;
            }
        }

        assertTrue(foundSubject, "Should find sent email with 'chicken' in subject");
        assertTrue(foundBody, "Should find sent email with 'chicken' in body");

        // Test search with term not present
        List<Email> emptyResults = emailManager.searchSent("testUser1", "nonexistentterm");
        assertTrue(emptyResults.isEmpty(), "Search for non-existent term should return empty list");

        // Test search for non-existent user
        List<Email> nonExistentUserResults = emailManager.searchSent("nonExistentUser", "chicken");
        assertTrue(nonExistentUserResults.isEmpty(), "Search for non-existent user should return empty list");
    }

    @Test
    void testGetEmailById() {
        // Send a test email
        Email sentEmail = emailManager.sendEmail("testUser1", "testUser2", "GetById Test", "Testing getEmailById");

        // Get all emails with IDs for the sender
        Map<Integer, Email> emailsWithIds = emailManager.getEmailIdsForUser("testUser1");

        // Find the ID of our test email
        Integer emailId = null;
        for (Map.Entry<Integer, Email> entry : emailsWithIds.entrySet()) {
            if (entry.getValue().getSubject().equals("GetById Test")) {
                emailId = entry.getKey();
                break;
            }
        }

        assertNotNull(emailId, "Should find ID for the test email");

        // Test getting the email by ID
        Optional<Email> retrievedEmail = emailManager.getEmailById(emailId, "testUser1");
        assertTrue(retrievedEmail.isPresent(), "Should retrieve email by ID for sender");
        assertEquals("GetById Test", retrievedEmail.get().getSubject(), "Retrieved email should have correct subject");

        // Test getting the email by ID as recipient
        Optional<Email> recipientRetrievedEmail = emailManager.getEmailById(emailId, "testUser2");
        assertTrue(recipientRetrievedEmail.isPresent(), "Should retrieve email by ID for recipient");

        // Test with invalid ID
        Optional<Email> nonExistentEmail = emailManager.getEmailById(99999, "testUser1");
        assertFalse(nonExistentEmail.isPresent(), "Should not find email with non-existent ID");

        // Test with unauthorized user
        Optional<Email> unauthorizedEmail = emailManager.getEmailById(emailId, "testUser3");
        assertFalse(unauthorizedEmail.isPresent(), "Unauthorized user should not be able to retrieve email");
    }

    @Test
    void testViewStatus() {
        // Send a test email
        Email sentEmail = emailManager.sendEmail("testUser1", "testUser2", "View Status Test", "Testing view status");

        // Initially, the email should be unviewed
        assertFalse(emailManager.hasRecipientViewedEmail(sentEmail, "testUser2"), "Email should initially be unviewed");

        // Get the email ID
        Map<Integer, Email> emailsWithIds = emailManager.getEmailIdsForUser("testUser1");
        Integer emailId = null;
        for (Map.Entry<Integer, Email> entry : emailsWithIds.entrySet()) {
            if (entry.getValue().getSubject().equals("View Status Test")) {
                emailId = entry.getKey();
                break;
            }
        }

        assertNotNull(emailId, "Should find ID for the test email");

        // Reading the email should mark it as viewed for the recipient
        Optional<Email> retrievedEmail = emailManager.getEmailById(emailId, "testUser2");
        assertTrue(retrievedEmail.isPresent(), "Should retrieve email by ID");

        // Check view status after reading
        assertTrue(emailManager.hasRecipientViewedEmail(sentEmail, "testUser2"), "Email should be marked as viewed after reading");

        // Check view status for all recipients
        Map<String, Boolean> viewStatus = emailManager.getViewStatusForAllRecipients(sentEmail);
        assertTrue(viewStatus.containsKey("testUser2"), "View status should include recipient");
        assertTrue(viewStatus.get("testUser2"), "Recipient should have viewed status true");
    }
}