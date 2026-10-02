package com.example.userservicefeb25.services;

import com.example.userservicefeb25.models.Token;
import com.example.userservicefeb25.models.User;

public interface UserService {
    Token login(String email, String password);

    User signUp(String name, String email, String password);


    void logout(String tokenValue);

    void verifyRegistrationOtp(String email, String otp);

    void resendRegistrationOtp(String email);

    void forgotPassword(String email);

    String verifyPasswordResetOtp(String email, String otp);

    void resetPassword(String resetToken, String newPassword);
}
