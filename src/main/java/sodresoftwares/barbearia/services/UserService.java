package sodresoftwares.barbearia.services;

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

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerClient(RegisterDTO data) {
        return createUser(data, UserRole.USER);
    }

    @Transactional
    public User createUser(RegisterDTO data, UserRole role) {
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
}