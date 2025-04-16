package com.example.demo.controller;


import com.example.demo.dto.LoginUserDto;
import com.example.demo.dto.RegisterUserDto;
import com.example.demo.dto.VerifyUserDto;
import com.example.demo.model.User;
import com.example.demo.response.LoginResponse;
import com.example.demo.service.AuthenticationService;
import com.example.demo.service.JwtService;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    public final AuthenticationService authenticationService;
    public final JwtService jwtService;

    public AuthenticationController(AuthenticationService authenticationService, JwtService jwtService){
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }

    @PostMapping("/signup")
    public ResponseEntity<User> signUp(@RequestBody RegisterUserDto registerUserDto) throws MessagingException {
        User registeredUser = authenticationService.signUp(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginUserDto loginUserDto){
        User authenticatedUser = authenticationService.authenticateUser(loginUserDto);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDto verifyUserDto){
        try{
            authenticationService.verifyUser(verifyUserDto);
            return ResponseEntity.ok("Account verified succesfully.");
        }catch(RuntimeException e){
            return ResponseEntity.badRequest().body("Some error occurred!" + e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException | MessagingException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
