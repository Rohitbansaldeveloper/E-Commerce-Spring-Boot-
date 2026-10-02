package com.example.userservicefeb25.controllers;

import com.example.userservicefeb25.dtos.*;
import com.example.userservicefeb25.models.Token;
import com.example.userservicefeb25.models.User;
import java.util.Map;
import com.example.userservicefeb25.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto) {
        try {
            Token token = userService.login(requestDto.getEmail(), requestDto.getPassword());
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            LoginResponseDto responseDto = new LoginResponseDto();
            responseDto.setTokenValue(token.getValue());
            return ResponseEntity.ok(responseDto);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signUp(@Valid @RequestBody SignUpRequestDto requestDto) {
        User user = userService.signUp(requestDto.getName(), requestDto.getEmail(), requestDto.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logOut(@Valid @RequestBody LogOutRequestDto requestDto) {
        userService.logout(requestDto.getTokenValue());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sample")
    public ResponseEntity<Void> sampleAPI() {
        return ResponseEntity.ok().build();
    }
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody OtpRequestDto requestDto) {
        userService.verifyRegistrationOtp(requestDto.getEmail(), requestDto.getOtp());
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(@Valid @RequestBody ForgotPasswordRequestDto requestDto) {
        userService.resendRegistrationOtp(requestDto.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto requestDto) {
        userService.forgotPassword(requestDto.getEmail());
        return ResponseEntity.ok(Map.of("message", "If the email is registered, a password reset OTP has been sent"));
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<Map<String, String>> verifyResetOtp(@Valid @RequestBody OtpRequestDto requestDto) {
        String resetToken = userService.verifyPasswordResetOtp(requestDto.getEmail(), requestDto.getOtp());
        if (resetToken == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        return ResponseEntity.ok(Map.of("resetToken", resetToken));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDto requestDto) {
        userService.resetPassword(requestDto.getResetToken(), requestDto.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

}
