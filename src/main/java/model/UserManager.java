package model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserManager implements IUserManager {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserManager() {
        bootstrapUsers();
    }

    private void bootstrapUsers() {
        register("testUser1", PasswordHasher.hashPassword("Password123"));
        register("testUser2", PasswordHasher.hashPassword("Password123"));
        register("testUser3", PasswordHasher.hashPassword("Password123"));
    }

    @Override
    public boolean register(String username, String passwordHash) {
        if (username == null || username.trim().isEmpty() || passwordHash == null || passwordHash.trim().isEmpty()) {
            return false;
        }

        if (userExists(username)) {
            return false;
        }

        User newUser = new User(username, passwordHash, new CopyOnWriteArrayList<>(), new CopyOnWriteArrayList<>());
        users.put(username, newUser);
        return true;
    }

    @Override
    public boolean authenticate(String username, String password) {
        User user = users.get(username);
        return user != null && PasswordHasher.verifyPassword(password, user.getPasswordHash());
    }

    @Override
    public boolean userExists(String username) {
        return users.containsKey(username);
    }


    public User getUserByUsername(String username) {
        return users.get(username);
    }

    public static String hashPassword(String password) {
        return PasswordHasher.hashPassword(password);
    }
}