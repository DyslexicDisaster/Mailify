package model;

import java.util.List;
import java.util.Optional;

public interface IEmailManager {
    Email sendEmail(String sender, List<String> recipients, String subject, String body);
    Email sendEmail(String sender, String recipient, String subject, String body);
    List<Email> listInbox(String username);
    List<Email> searchInbox(String username, String term);
    List<Email> listSent(String username);
    List<Email> searchSent(String username, String term);
    Optional<Email> getEmailById(int id, String username);
}
