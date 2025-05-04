package model;

public interface IUserManager {
    boolean register(String username, String passwordHash);
    boolean authenticate(String username, String password);
    boolean userExists(String username);
    User getUserByUsername(String username);
}
