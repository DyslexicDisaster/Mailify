package model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages user registration, authentication, and lookup in-memory.
 * Utilizes thread-safe collections to allow concurrent access.
 */
public class UserManager implements IUserManager {
    /**
     * Thread-safe map storing username to User object.
     */
    private final Map<String, User> users = new ConcurrentHashMap<>();

    /**
     * Constructs a UserManager and bootstraps default test users.
     */
    public UserManager() {
        bootstrapUsers();
    }

    /**
     * Populates the system with initial test users.
     */
    private void bootstrapUsers() {
        register("testUser1", PasswordHasher.hashPassword("Password123"));
        register("testUser2", PasswordHasher.hashPassword("Password123"));
        register("testUser3", PasswordHasher.hashPassword("Password123"));
    }

    /**
     * Registers a new user with a hashed password.
     *
     * @param username     the desired username (non-null, non-empty)
     * @param passwordHash the already-hashed password string
     * @return true if registration succeeds; false if username is invalid or already exists
     */
    @Override
    public boolean register(String username, String passwordHash) {
        if (username == null || username.trim().isEmpty() || passwordHash == null || passwordHash.trim().isEmpty()) {
            return false;
        }

        if (userExists(username)) {
            return false;
        }

        User newUser = new User(
                username,
                passwordHash,
                new CopyOnWriteArrayList<>(),
                new CopyOnWriteArrayList<>(),
                new CopyOnWriteArraySet<>()
        );
        users.put(username, newUser);
        return true;
    }

    /**
     * Authenticates a user by verifying the provided password against the stored hash.
     *
     * @param username the username of the user to authenticate
     * @param password the plaintext password provided for authentication
     * @return true if authentication succeeds; false otherwise
     */
    @Override
    public boolean authenticate(String username, String password) {
        User user = users.get(username);
        return user != null && PasswordHasher.verifyPassword(password, user.getPasswordHash());
    }

    /**
     * Checks if a user with the given username exists in the system.
     *
     * @param username the username to check
     * @return true if the user exists; false otherwise
     */
    @Override
    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    /**
     * Retrieves the User object associated with the given username.
     *
     * @param username the username of the user to retrieve
     * @return the User object, or null if not found
     */
    public User getUserByUsername(String username) {
        return users.get(username);
    }

    /**
     * Utility method for hashing a plaintext password using the PasswordHasher.
     *
     * @param password the plaintext password to hash
     * @return the hashed password string
     */
    public static String hashPassword(String password) {
        return PasswordHasher.hashPassword(password);
    }
}
