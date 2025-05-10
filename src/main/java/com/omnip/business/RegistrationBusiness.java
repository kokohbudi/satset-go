package com.omnip.business;

import com.omnip.repositories.StoreRepository;
import com.omnip.repositories.UsersRepository;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Random;

@Component
public class RegistrationBusiness {
    public boolean isEmailRegistered(String email, UsersRepository usersRepository, StoreRepository storeRepository) {
        return usersRepository.findByEmail(email) != null || storeRepository.findByEmail(email) != null;
    }

    public String generateReferralId(String fullName, StoreRepository storeRepository, Random random) {
        String normalizedName = this.normalizeString(fullName);
        String[] nameParts = normalizedName.split("\\s+");
        StringBuilder referralId = new StringBuilder();
        if (nameParts.length >= 2) {
            referralId.append(nameParts[0].substring(0, 1).toUpperCase());
            String lastName = nameParts[nameParts.length - 1];
            referralId.append(lastName.substring(0, Math.min(5, lastName.length())).toUpperCase());
        } else if (nameParts.length == 1) {
            referralId.append(nameParts[0].substring(0, Math.min(6, nameParts[0].length())).toUpperCase());
        }
        referralId.append(String.format("%04d", random.nextInt(10000)));
        String candidateId = referralId.toString();
        int attempt = 0;
        while (storeRepository.existsByReferralId(candidateId) && attempt < 10) {
            candidateId = referralId.substring(0, referralId.length() - 4) +
                    String.format("%04d", random.nextInt(10000));
            attempt++;
        }
        if (storeRepository.existsByReferralId(candidateId)) {
            candidateId = referralId.substring(0, referralId.length() - 4) +
                    this.generateRandomString(6, random);
        }
        return candidateId;
    }

    private String normalizeString(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replaceAll("[^a-zA-Z\\s]", "");
        return normalized.toLowerCase();
    }

    /**
     * Generates a random alphanumeric string of the specified length
     */
    private String generateRandomString(int length, Random random) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(random.nextInt(characters.length())));
        }
        return result.toString();
    }
}
