package sodresoftwares.barbearia.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.ChangePasswordDTO;
import sodresoftwares.barbearia.dto.RegisterDTO;
import sodresoftwares.barbearia.dto.UpdateUserDTO;
import sodresoftwares.barbearia.dto.UserResponseDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.model.user.UserRole;
import sodresoftwares.barbearia.repositories.UserRepository;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LgpdConsentService lgpdConsentService;

    @Transactional(readOnly = true)
    public UserResponseDTO getMyProfile(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    return new AppException(
                            HttpStatus.NOT_FOUND,
                            "USER_NOT_FOUND",
                            "User not found."
                    );
                });

        return UserResponseDTO.fromEntity(user);
    }

    @Transactional
    public User registerClient(RegisterDTO data, HttpServletRequest request) {
        return createUser(data, UserRole.USER,request);
    }

    @Transactional
    public User createUser(RegisterDTO data, UserRole role, HttpServletRequest request) {
        if (this.userRepository.existsByLogin(data.login())) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "USER_ALREADY_EXISTS",
                    "User already exists");
        }

        String encryptedPassword = passwordEncoder.encode(data.password());

        User newUser = User.builder()
                .login(data.login())
                .password(encryptedPassword)
                .name(data.name())
                .phone(data.phone())
                .role(role)
                .build();

        User savedUser = userRepository.save(newUser);
        lgpdConsentService.registerConsentForNewUser(savedUser, request);
        log.info("User registered with role {}", savedUser.getRole());

        return savedUser;
    }

    @Transactional
    public UserResponseDTO updateUserProfile(String userId, UpdateUserDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    return new AppException(
                            HttpStatus.NOT_FOUND,
                            "USER_NOT_FOUND",
                            "User not found."
                    );
                });

        if (dto.name() != null && !dto.name().isBlank()) {
            user.setName(dto.name().trim());
        }

        if (dto.phone() != null) {
            user.setPhone(dto.phone().trim());
        }

        User updatedUser = userRepository.save(user);

        log.info("User updated successfully");
        return UserResponseDTO.fromEntity(updatedUser);
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordDTO dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    return new AppException(
                            HttpStatus.NOT_FOUND,
                            "USER_NOT_FOUND",
                            "User not found."
                    );
                });

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CURRENT_PASSWORD",
                    "Current password does not match."
            );
        }

        if (!dto.newPassword().equals(dto.confirmPassword())) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORDS_DO_NOT_MATCH",
                    "New password and confirmation do not match."
            );
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        log.info("Password changed successfully");
    }

    @Transactional
    public void deleteMyAccount(String loggedUserId) {
        User user = userRepository.findById(loggedUserId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found in the system."
                ));

        if (!user.getIsActive()) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "ALREADY_DELETED",
                    "This account is already deactivated."
            );
        }

        user.setIsActive(false);
        user.setDeletedAt(Instant.now());

        userRepository.save(user);

        log.info("Account deactivated successfully. Permanent deletion pending.");
    }

    @Transactional
    public void reactivateAccount(String loggedUserId) {
        User user = userRepository.findById(loggedUserId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found in the system."
                ));

        if (user.getIsActive()) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "ALREADY_ACTIVE",
                    "This account is already active."
            );
        }

        user.setIsActive(true);
        user.setDeletedAt(null);

        userRepository.save(user);

        log.info("Account reactivated successfully.");
    }
}