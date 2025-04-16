package com.example.demo.service;

import com.example.demo.dto.LoginUserDto;
import com.example.demo.dto.RegisterUserDto;
import com.example.demo.dto.VerifyUserDto;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
//import jakarta.mail.MessagingException; used for try-catch logic
import jakarta.mail.MessagingException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    //constructor
    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public User signUp(RegisterUserDto input) throws MessagingException {
        User user = new User(input.getUsername(), input.getEmail(), passwordEncoder.encode(input.getPassword()));
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(30));
        user.setEnabled(false);
        sendVerificationEmail(user);
        return userRepository.save(user);

    }

    public User authenticateUser(LoginUserDto input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(()->new RuntimeException("User not found!"));

        if(!user.isEnabled()){
            throw new RuntimeException("Account is not verified. Please verify your account first.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword())
        );

        return user;
    }

    public void verifyUser(VerifyUserDto input){
        Optional<User> optionalUser = userRepository.findByEmail(input.getEmail());

        if(optionalUser.isPresent()){
            User user = optionalUser.get();
            if(user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())){
                throw new RuntimeException("Verification code has already expired.");
            }

            if(user.getVerificationCode().equals(input.getVerificationCode())){
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                userRepository.save(user);
            } else {
                throw new RuntimeException("Incorrect verification code.");
            }
        } else {
            throw new RuntimeException("User doesn't exist.");
        }
    }

    public void resendVerificationCode(String email) throws MessagingException {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if(optionalUser.isPresent()){
            User user = optionalUser.get();

            if(user.isEnabled()){
                throw new RuntimeException("Account is already verified.");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
            sendVerificationEmail(user);
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found.");
        }
    }

    private void sendVerificationEmail(User user) throws MessagingException {
        String subject = "Account Verification";
        String verificationCode = "VERIFICATION CODE " + user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        //NOTE: ADD THIS SENDING PART TO try-catch BLOCK TO HANDLE MessagingException;
        emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
    }

    private String generateVerificationCode(){
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
