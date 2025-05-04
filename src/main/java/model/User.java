package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String username;
    private String passwordHash;
    private List<Email> inbox = new CopyOnWriteArrayList<>();
    private List<Email> sent = new CopyOnWriteArrayList<>();
    private Set<Email> viewedEmails = new CopyOnWriteArraySet<>();

    public boolean hasViewedEmail(Email email) {
        return viewedEmails.contains(email);
    }

    public void markEmailAsViewed(Email email) {
        viewedEmails.add(email);
    }
}
