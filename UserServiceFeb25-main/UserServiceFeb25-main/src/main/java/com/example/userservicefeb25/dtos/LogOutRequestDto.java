package com.example.userservicefeb25.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogOutRequestDto {
    @NotBlank(message = "Token is required")
    private String tokenValue;
}
