package com.example.cartservicefeb25.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InternalServiceAuthInterceptor implements ClientHttpRequestInterceptor {
    private final String token;
    public InternalServiceAuthInterceptor(@Value("${internal.service.token}") String token) { this.token = token; }
    @Override public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set("X-Service-Token", token);
        return execution.execute(request, body);
    }
}
