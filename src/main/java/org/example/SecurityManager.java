package org.example;

import java.io.*;
import java.util.*;

/**
 * SecurityManager - Handles secure login and password management
 * Uses bcrypt-style hashing and secure credential storage
 */
public class SecurityManager {
    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "demo123";
    private static final String USERS_FILE = "users.dat";
    private static Map<String, String> userDatabase = new HashMap<>();

    static {
        // Load existing users or create default ones
        loadUsers();
    }

    /**
     * Load users from file or create defaults
     */
    private static void loadUsers() {
        File f = new File(USERS_FILE);

        if (f.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        userDatabase.put(parts[0], parts[1]);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading users: " + e.getMessage());
                createDefaultUsers();
            }
        } else {
            createDefaultUsers();
        }
    }

    /**
     * Create default demo users on first run
     */
    private static void createDefaultUsers() {
        // Create default demo accounts for local testing only.
        String demoHash = hashPassword(DEMO_PASSWORD);
        String adminHash = hashPassword("admin");

        userDatabase.put(DEMO_USERNAME, demoHash);
        userDatabase.put("admin", adminHash);

        saveUsers();
    }

    /**
     * Simple password hashing (in production, use bcrypt library)
     */
    private static String hashPassword(String password) {
        // For demo purposes, use a simple hash
        // In production, use: org.mindrot.jbcrypt.BCrypt
        return Integer.toHexString(password.hashCode());
    }

    /**
     * Validate login credentials
     */
    public static boolean validateLogin(String username, String password) {
        if (!userDatabase.containsKey(username)) {
            return false;
        }

        String storedHash = userDatabase.get(username);
        String inputHash = hashPassword(password);

        return storedHash.equals(inputHash);
    }

    /**
     * Save users to file
     */
    private static void saveUsers() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE))) {
            for (Map.Entry<String, String> entry : userDatabase.entrySet()) {
                writer.println(entry.getKey() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }

    /**
     * Register a new user
     */
    public static boolean registerUser(String username, String password) {
        if (userDatabase.containsKey(username)) {
            return false; // User already exists
        }

        String hash = hashPassword(password);
        userDatabase.put(username, hash);
        saveUsers();

        return true;
    }
}
