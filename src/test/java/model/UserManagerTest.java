package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {
    private UserManager userManager;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
    }

    @Test
    void testBootstrappedUsers() {
        assertTrue(userManager.userExists("testUser1"));
        assertTrue(userManager.userExists("testUser2"));
        assertTrue(userManager.userExists("testUser3"));

        User user1 = userManager.getUserByUsername("testUser1");
        assertNotNull(user1);
        assertEquals("testUser1", user1.getUsername());
        assertNotNull(user1.getPasswordHash());
        assertNotNull(user1.getInbox());
        assertNotNull(user1.getSent());
    }

    @Test
    void testRegisterNewUser() {
        String username = "newUser";
        String passwordHash = PasswordHasher.hashPassword("Password123");

        assertTrue(userManager.register(username, passwordHash));
        assertTrue(userManager.userExists(username));

        assertFalse(userManager.register(username, passwordHash));
    }

    @Test
    void testRegisterInvalidData() {
        // Test null username
        assertFalse(userManager.register(null, "hash"));

        // Test empty username
        assertFalse(userManager.register("", "hash"));
        assertFalse(userManager.register("  ", "hash"));

        // Test null password hash
        assertFalse(userManager.register("validUser", null));

        // Test empty password hash
        assertFalse(userManager.register("validUser", ""));
        assertFalse(userManager.register("validUser", "  "));
    }

    @Test
    void testAuthentication() {
        // Test with bootstrap user
        assertTrue(userManager.authenticate("testUser1", "Password123"));

        // Test with wrong password
        assertFalse(userManager.authenticate("testUser1", "WrongPassword"));

        // Test with non-existent user
        assertFalse(userManager.authenticate("nonExistentUser", "Password123"));

        // Register a new user
        String username = "authTestUser";
        String password = "Password123";
        String passwordHash = PasswordHasher.hashPassword(password);

        userManager.register(username, passwordHash);

        // Test authentication with the new user
        assertTrue(userManager.authenticate(username, password));
        assertFalse(userManager.authenticate(username, "WrongPassword"));
    }

    @Test
    void testUserExistsMethod() {
        // Test with existing users
        assertTrue(userManager.userExists("testUser1"));
        assertTrue(userManager.userExists("testUser2"));
        assertTrue(userManager.userExists("testUser3"));

        // Test with non-existent user
        assertFalse(userManager.userExists("testUser1717"));

        assertFalse(userManager.userExists(""));
    }

    @Test
    void testGetUserByUsername() {
        // Test with existing user
        User user = userManager.getUserByUsername("testUser1");
        assertNotNull(user);
        assertEquals("testUser1", user.getUsername());

        // Test with non-existent user
        assertNull(userManager.getUserByUsername("nonExistentUser"));

        // Register new user and get it
        String newUsername = "getUserTest";
        userManager.register(newUsername, PasswordHasher.hashPassword("password"));

        User newUser = userManager.getUserByUsername(newUsername);
        assertNotNull(newUser);
        assertEquals(newUsername, newUser.getUsername());
    }

}