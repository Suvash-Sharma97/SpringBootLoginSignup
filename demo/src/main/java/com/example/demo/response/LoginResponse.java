package com.example.demo.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String jwtToken;
    private long expirationTime;

    public LoginResponse(String jwtToken, long expirationTime) {
        this.jwtToken = jwtToken;
        this.expirationTime = expirationTime;
    }
}
