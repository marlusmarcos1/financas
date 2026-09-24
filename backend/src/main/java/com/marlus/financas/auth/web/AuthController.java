package com.marlus.financas.auth.web;

import com.marlus.financas.auth.service.AppUserDetails;
import com.marlus.financas.auth.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthenticationManager authenticationManager, LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
    }

    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void csrf(HttpServletRequest request) {
        // Força a resolução do token diferido, o que grava o cookie XSRF-TOKEN na resposta.
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken();
        }
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (loginAttemptService.isBlocked(request.username())) {
            throw new LockedException("Usuário temporariamente bloqueado por excesso de tentativas.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            loginAttemptService.loginSucceeded(request.username());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            httpRequest
                    .getSession(true)
                    .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
            return new UserResponse(principal.getId(), principal.getUsername(), principal.getDisplayName());
        } catch (BadCredentialsException ex) {
            loginAttemptService.loginFailed(request.username());
            throw ex;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler()
                .logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AppUserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new UserResponse(principal.getId(), principal.getUsername(), principal.getDisplayName()));
    }
}
