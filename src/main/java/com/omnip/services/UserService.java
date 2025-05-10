package com.omnip.services;

import com.omnip.BusinessException;
import com.omnip.business.UserManagementBusiness;
import com.omnip.dto.UserDTO;
import com.omnip.entities.Users;
import com.omnip.repositories.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UsersRepository usersRepository;
    private final UserManagementBusiness userManagementBusiness;


    public UserService(UsersRepository usersRepository, UserManagementBusiness userManagementBusiness) {
        this.usersRepository = usersRepository;
        this.userManagementBusiness = userManagementBusiness;
    }

    /**
     * Generates a unique referral ID based on the user's full name
     *
     * @param fullName the user for whom to generate a referral ID
     * @return the generated referral ID
     */


    /**
     * Normalizes a string by removing accents and special characters
     */


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

    public Users createNewUser(Users user) {

        return this.usersRepository.save(user);
    }

    public Users findByEmail(String email) {
        return this.usersRepository.findByEmail(email);
    }

    public Users findByProviderUserId(String providerUserId) {
        return this.usersRepository.findByProviderUserId(providerUserId);
    }

    public String getProviderUseIdChangePassword(UserDTO sessionUserDTO, UserDTO requestUserDTO, List allowedRoles) throws BusinessException {
        return this.userManagementBusiness.getProviderUseIdChangePassword(sessionUserDTO, requestUserDTO, this.usersRepository, allowedRoles);
    }
}
