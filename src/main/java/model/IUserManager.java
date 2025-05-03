package model;

public interface IUserManager {
    boolean register(String username, String passwordHash);
    boolean authenticate(String username, String passwordHash);
    boolean userExists(String username);
}
