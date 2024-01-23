package com.example.epamProject.service;

import com.example.epamProject.dto.RegistrationDto;
import com.example.epamProject.entity.ConfirmationTokenEntity;
import com.example.epamProject.entity.RoleEntity;
import com.example.epamProject.entity.UserEntity;
import com.example.epamProject.repo.ConfirmationTokenRepository;
import com.example.epamProject.repo.RoleRepository;
import com.example.epamProject.repo.UserRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegistrationService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private static final String CONFIRM_ACCOUNT_URL = "http://localhost:8080/confirm-account?token=";
    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    public RegistrationService(UserRepository userRepository, PasswordEncoder encoder, RoleRepository roleRepository, EmailService emailService, ConfirmationTokenRepository confirmationTokenRepository) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
        this.confirmationTokenRepository = confirmationTokenRepository;
    }

    @Transactional
    public ResponseEntity<String> registerUser(RegistrationDto userDto) {
        logger.info("Registering user with email: {}", userDto.getEmail());

        if (userRepository.existsByEmail(userDto.getEmail())) {
            logger.warn("Email {} is already in use", userDto.getEmail());

            return new ResponseEntity<>("Email is already in use", HttpStatus.NOT_ACCEPTABLE);
        }
        if (userDto.getPassword().isEmpty() || userDto.getLastName().isEmpty() || userDto.getEmail().isEmpty() ||
                userDto.getFirstName().isEmpty()) {
            logger.warn("Fields cannot be empty for user with email: {}", userDto.getEmail());
            return new ResponseEntity<>("Fields cannot be empty", HttpStatus.BAD_REQUEST);
        }
        if (!isPasswordStrong(userDto.getPassword())) {
            logger.warn("Password does not meet strength requirements for user with email: {}", userDto.getEmail());
            return new ResponseEntity<>("Password does not meet strength requirements", HttpStatus.BAD_REQUEST);
        }
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            logger.warn("Password and confirm password does not match for user with email: {}", userDto.getEmail());
            return new ResponseEntity<>("Passwords do not match", HttpStatus.BAD_REQUEST);
        }
        if(!isValidEmailAddress(userDto.getEmail())){
            logger.warn("The provided email does not exist : {}", userDto.getEmail());

            return new ResponseEntity<>("The email address you provided doesn't exist", HttpStatus.BAD_REQUEST);
        }


        UserEntity user = new UserEntity();
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setPassword(encoder.encode(userDto.getPassword()));  // Encode the password
        RoleEntity userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role not found"));  // Adjust the role name accordingly
        user.setRole(userRole);

        // Save the user to the database
        userRepository.save(user);
        String token = UUID.randomUUID().toString();
        ConfirmationTokenEntity confirmationToken = new ConfirmationTokenEntity();
        confirmationToken.setUser(user);
        confirmationToken.setConfirmationToken(token);
        confirmationTokenRepository.save(confirmationToken);

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject("Complete Registration!");
        mailMessage.setText("To confirm your account, please click here : "
                + CONFIRM_ACCOUNT_URL + confirmationToken.getConfirmationToken());
        emailService.sendEmail(mailMessage);

        logger.info("Confirmation Token: {}", confirmationToken.getConfirmationToken());

        return ResponseEntity.ok("Verify email by the link sent on your email address");

    }

    public String confirmEmail(String confirmationToken) {
        ConfirmationTokenEntity token = confirmationTokenRepository.findByConfirmationToken(confirmationToken);

        if (token != null) {
            UserEntity user = userRepository.findByEmail(token.getUser().getEmail());
            user.setEnabled(true);
            userRepository.save(user);
            return "success";
        }
        return "failed";
    }
    private boolean isPasswordStrong(String password) {
        // Implement your password strength check logic here
        // Example: At least 8 characters, containing at least one uppercase letter, one lowercase letter, and one digit
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
    }
    public static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

}