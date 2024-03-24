package com.example.epamProject.controller;

import java.util.UUID;

import com.example.epamProject.config.JwtRequest;
import com.example.epamProject.config.JwtResponse;
import com.example.epamProject.config.JwtTokUtil;
import com.example.epamProject.config.JwtUserDetailService;
import com.example.epamProject.entity.SessionEntity;
import com.example.epamProject.repo.SessionRepository;
import com.example.epamProject.service.JwtAuthenticationService;
import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwtAuthenticationController
{

    private final JwtTokUtil jwtTokUtil;

    private final JwtUserDetailService userDetailsService;

    private final JwtAuthenticationService authenticationService;

    private final SessionRepository sessionRepository;

    public JwtAuthenticationController(
        JwtTokUtil jwtTokUtil,
        JwtUserDetailService userDetailsService,
        JwtAuthenticationService authenticationService,
        SessionRepository sessionRepository
    )
    {
        this.jwtTokUtil = jwtTokUtil;
        this.userDetailsService = userDetailsService;
        this.authenticationService = authenticationService;
        this.sessionRepository = sessionRepository;
    }

    @PostMapping(value = "/authenticate")
    @CrossOrigin(origins = "http://localhost:63342")
    @Transactional
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest, HttpServletRequest request)
    {
        System.out.println(authenticationRequest.getUsername());
        UserAgent userAgent = UserAgent.parseUserAgentString(request.getHeader("User-Agent"));
        try
        {
            authenticationService.authenticate(
                authenticationRequest.getUsername(),
                authenticationRequest.getPassword()
            );
            final UserDetails userDetails = userDetailsService
                .loadUserByUsername(authenticationRequest.getUsername());

            final String token = jwtTokUtil.generateToken(userDetails.getUsername());
            SessionEntity sessionEntity = new SessionEntity();
            sessionEntity.setToken(token);
            sessionEntity.setEmail(authenticationRequest.getUsername());
            var sessionId = UUID.randomUUID();
            sessionEntity.setSessionId(sessionId.toString());
            sessionEntity.setBrowserName(userAgent.getBrowser().getName());
            this.sessionRepository.save(sessionEntity);

            return ResponseEntity.ok(new JwtResponse(token));
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Invalid login or password");
        }
    }
}