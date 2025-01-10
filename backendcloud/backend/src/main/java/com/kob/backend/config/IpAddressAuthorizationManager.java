package com.kob.backend.config;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;

@Component
public class IpAddressAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String ALLOWED_IP = "127.0.0.1";

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        String remoteAddress = request.getRemoteAddr();
        return new AuthorizationDecision(ALLOWED_IP.equals(remoteAddress));
    }
}