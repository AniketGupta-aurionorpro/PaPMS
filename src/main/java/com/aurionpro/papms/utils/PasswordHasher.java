package com.aurionpro.papms.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHasher {
    public static void main(String[] args) {
        // The password you want to hash
        String plainPassword = "password123"; 

        // Create an instance of the password encoder
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Generate the hashed password
        String hashedPassword = encoder.encode(plainPassword);

        // Print the result to the console
        System.out.println("Plain Text Password: " + plainPassword);
        System.out.println("Hashed Password: " + hashedPassword);
    }
}