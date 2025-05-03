package com.omnip.services;

import com.omnip.constant.OmniConstants;
import com.omnip.entities.Users;
import com.omnip.repositories.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UsersRepository usersRepository;
    private final Random random = new SecureRandom();

    public UserService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    /**
     * Generates a unique referral ID based on the user's full name
     * @param fullName the user for whom to generate a referral ID
     * @return the generated referral ID
     */
    public String generateReferralId(String fullName) {
                // Normalize the full name (remove accents, convert to lowercase)
        String normalizedName = this.normalizeString(fullName);

        // Extract name parts
        String[] nameParts = normalizedName.split("\\s+");

        // Build initial referral code
        StringBuilder referralId = new StringBuilder();

        if (nameParts.length >= 2) {
            // If there are at least 2 name parts, use first letter of first name + up to 5 chars of last name
            referralId.append(nameParts[0].substring(0, 1).toUpperCase());

            // Add up to 5 characters from the last name
            String lastName = nameParts[nameParts.length - 1];
            referralId.append(lastName.substring(0, Math.min(5, lastName.length())).toUpperCase());
        } else if (nameParts.length == 1) {
            // If only one name part, use up to 6 chars
            referralId.append(nameParts[0].substring(0, Math.min(6, nameParts[0].length())).toUpperCase());
        }

        // Add a random 4-digit number
        referralId.append(String.format("%04d", this.random.nextInt(10000)));

        // Ensure uniqueness by checking against existing referral IDs
        String candidateId = referralId.toString();
        int attempt = 0;

        while (this.usersRepository.existsByReferalId(candidateId) && attempt < 10) {
            // If the referral ID already exists, generate a new random suffix
            candidateId = referralId.substring(0, referralId.length() - 4) +
                    String.format("%04d", this.random.nextInt(10000));
            attempt++;
        }

        // If we still have a collision after several attempts, add more random characters
        if (this.usersRepository.existsByReferalId(candidateId)) {
            candidateId = referralId.substring(0, referralId.length() - 4) +
                    this.generateRandomString(6);
        }

        return candidateId;
    }

    /**
     * Normalizes a string by removing accents and special characters
     */
    private String normalizeString(String input) {
        // Normalize to NFD form and remove accents
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Remove special characters, keep only letters and spaces
        normalized = normalized.replaceAll("[^a-zA-Z\\s]", "");

        // Convert to lowercase
        return normalized.toLowerCase();
    }

    /**
     * Generates a random alphanumeric string of the specified length
     */
    private String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(this.random.nextInt(characters.length())));
        }

        return result.toString();
    }

    public void updateExistingUser(Users user, String username, String fullName) {
        // Update user information if needed
        boolean changed = false;

        if (username != null && !username.equals(user.getUsername())) {
            user.setUsername(username);
            changed = true;
        }

        if (fullName != null && !fullName.equals(user.getFullname())) {
            user.setFullname(fullName);
            changed = true;
        }

        // Ensure user is active
        if (!user.isActive()) {
            user.setActive(true);
            changed = true;
        }

        if (changed) {
            user.setUpdatedAt(LocalDateTime.now());
            this.usersRepository.save(user);
            logger.info("Updated existing user record for: {}", user.getEmail());
        }
    }

    public void createNewUser(String email, String username, String fullName,String referalId, List<String> roles) {
        Users newUser = new Users();
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setFullname(fullName);
        newUser.setReferalId(referalId);
        newUser.setRegistrationChannel(OmniConstants.REGISTRATION_CHANNEL_KEYCLOAK);
        newUser.setActive(true);

        newUser.setRoles(roles);

        this.usersRepository.save(newUser);
        logger.info("Created new user record for: {}", email);
    }
}
