package com.rhodebazar.backend.security;

import com.rhodebazar.backend.domain.utilisateur.UtilisateurRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurDetailsService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .map(UtilisateurPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));
    }
}
