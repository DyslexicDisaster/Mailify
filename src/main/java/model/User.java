package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String username;
    private String passwordHash;
    private List<Email> inbox = new CopyOnWriteArrayList<>();
    private List<Email> sent = new CopyOnWriteArrayList<>();
}
