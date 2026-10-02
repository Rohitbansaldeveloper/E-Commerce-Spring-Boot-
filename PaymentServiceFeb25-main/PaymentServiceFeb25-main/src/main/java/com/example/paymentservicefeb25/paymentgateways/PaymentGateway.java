package com.example.paymentservicefeb25.paymentgateways;

import com.stripe.exception.StripeException;

public interface PaymentGateway {
    String initiatePayment(Long orderId, java.math.BigDecimal amount) throws StripeException;
}
