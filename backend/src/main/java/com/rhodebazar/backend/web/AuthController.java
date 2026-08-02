package com.rhodebazar.backend.web;

import com.rhodebazar.backend.security.JwtService;
import com.rhodebazar.backend.security.UtilisateurPrincipal;
import com.rhodebazar.backend.web.dto.LoginRequest;
import com.rhodebazar.backend.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest requete) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requete.email(), requete.motDePasse()));

        UtilisateurPrincipal principal = (UtilisateurPrincipal) authentication.getPrincipal();
        String role = principal.getUtilisateur().getRole().name();
        String token = jwtService.genererToken(principal.getUsername(), role);

        return new LoginResponse(
                token,
                principal.getUtilisateur().getNom(),
                principal.getUsername(),
                role);
    }
}
