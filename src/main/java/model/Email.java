package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Email {
    private String sender;
    private List<String> recipients;
    private String subject;
    private String body;
    private LocalDateTime timestamp;
    private boolean viewed;
}
