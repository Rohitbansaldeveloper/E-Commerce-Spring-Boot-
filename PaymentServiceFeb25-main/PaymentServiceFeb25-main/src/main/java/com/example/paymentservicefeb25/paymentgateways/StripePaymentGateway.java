package com.example.paymentservicefeb25.paymentgateways;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentLink;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.param.PaymentLinkCreateParams;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.ProductCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class StripePaymentGateway implements PaymentGateway {
    @Value("${stripe.key:}")
    private String apiKey;

    @Value("${payment.currency:INR}")
    private String currency;

    
    @Value("${payment.success-url:http://ecommerce.local/payment/success}")
    private String successUrl;

    @Override
    public String initiatePayment(Long orderId, java.math.BigDecimal amount) throws StripeException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("STRIPE_KEY is not configured");
        }
        Stripe.apiKey = apiKey;

        ProductCreateParams productCreateParams =
                ProductCreateParams.builder().setName("Order #" + orderId).build();
        Product product = Product.create(productCreateParams);

        //55.78 => 5578L => 55.78
        // 67.32 => 67.32 * 100 => 6732
        PriceCreateParams priceCreateParams =
                PriceCreateParams.builder()
                        .setCurrency(currency)
                        .setUnitAmount(amount.multiply(java.math.BigDecimal.valueOf(100)).longValueExact())
                        .setProduct(product.getId().toString())
                        .build();

        Price price = Price.create(priceCreateParams);

        PaymentLinkCreateParams paymentLinkCreateParams =
                PaymentLinkCreateParams.builder()
                        .putMetadata("orderId", String.valueOf(orderId))
                        .addLineItem(
                                PaymentLinkCreateParams.LineItem.builder()
                                        .setPrice(price.getId().toString())
                                        .setQuantity(1L)
                                        .build()
                        )
                        .setAfterCompletion(
                                PaymentLinkCreateParams.AfterCompletion.builder()
                                        .setType(PaymentLinkCreateParams.AfterCompletion.Type.REDIRECT) // CALLBACK
                                        .setRedirect(
                                                PaymentLinkCreateParams.AfterCompletion.Redirect.builder()
                                                        .setUrl(successUrl)
                                                        .build()
                                        )
                                        .build()
                        )
                        .build();

        PaymentLink paymentLink = PaymentLink.create(paymentLinkCreateParams);

        return paymentLink.getUrl();
    }
}


