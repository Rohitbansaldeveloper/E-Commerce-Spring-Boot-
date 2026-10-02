package com.example.paymentservicefeb25.dtos;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
public record PaymentInitiateRequest(@NotNull @Positive BigDecimal amount) {}
