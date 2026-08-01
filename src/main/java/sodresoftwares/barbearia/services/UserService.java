package sodresoftwares.barbearia.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sodresoftwares.barbearia.dto.ChangePasswordDTO;
import sodresoftwares.barbearia.dto.UpdateUserDTO;
import sodresoftwares.barbearia.dto.UserResponseDTO;
import sodresoftwares.barbearia.infra.exception.AppException;
import sodresoftwares.barbearia.model.user.User;
import sodresoftwares.barbearia.repositories.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponseDTO getMyProfile(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found");
                    return new AppException(
                            HttpStatus.NOT_FOUND,
                            "USER_NOT_FOUND",
                            "User not found."
                    );
                });

        return UserResponseDTO.fromEntity(user);
    }

    @Transactional
    public UserResponseDTO updateUserProfile(String userId, UpdateUserDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User update failed. User not found");
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
                    log.warn("Password change failed. User not found");
                    return new AppException(
                            HttpStatus.NOT_FOUND,
                            "USER_NOT_FOUND",
                            "User not found."
                    );
                });

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            log.warn("Password change failed. Incorrect current password");
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CURRENT_PASSWORD",
                    "Current password does not match."
            );
        }

        if (!dto.newPassword().equals(dto.confirmPassword())) {
            log.warn("Password change failed. New passwords do not match");
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
}