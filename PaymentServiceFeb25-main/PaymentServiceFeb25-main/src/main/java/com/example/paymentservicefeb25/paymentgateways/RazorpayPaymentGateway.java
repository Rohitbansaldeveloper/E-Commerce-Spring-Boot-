package com.example.paymentservicefeb25.paymentgateways;

import org.springframework.stereotype.Component;

@Component
public class RazorpayPaymentGateway implements PaymentGateway {
    @Override
    public String initiatePayment(Long orderId) {
        return null;
    }
}
