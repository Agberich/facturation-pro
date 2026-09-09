package com.facturation.service;

import com.facturation.dto.*;
import com.facturation.entity.Entreprise;
import com.facturation.entity.Parametre;
import com.facturation.entity.Utilisateur;
import com.facturation.repository.EntrepriseRepository;
import com.facturation.repository.ParametreRepository;
import com.facturation.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final ParametreRepository parametreRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public PremierAdminExisteResponseDTO verifierPremierAdminExiste() {
        boolean existe = utilisateurRepository.existsByDeletedAtIsNull();
        return PremierAdminExisteResponseDTO.builder()
                .existe(existe)
                .build();
    }

    @Transactional
    public LoginResponseDTO creerPremierAdmin(PremierAdminRequestDTO request) {
        if (utilisateurRepository.existsByDeletedAtIsNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un administrateur existe déjà dans le système.");
        }

        // 1. Création de l'entreprise
        Entreprise entreprise = new Entreprise();
        entreprise.setNom(request.getNomEntreprise());
        entreprise = entrepriseRepository.save(entreprise);

        // 2. Initialisation des paramètres par défaut
        Parametre parametre = new Parametre();
        parametre.setEntreprise(entreprise);
        parametreRepository.save(parametre);

        // 3. Création de l'utilisateur Admin
        Utilisateur admin = new Utilisateur();
        admin.setNom(request.getNom());
        admin.setPrenom(request.getPrenom());
        admin.setEmail(request.getEmail());
        admin.setTelephone(request.getTelephone());
        admin.setMotDePasseHash(passwordEncoder.encode(request.getMotDePasse()));
        admin.setRole(Utilisateur.RoleUtilisateur.ADMIN);
        admin.setEntreprise(entreprise);
        admin.setActif(true);
        admin.setDoitChangerMotDePasse(false);
        admin = utilisateurRepository.save(admin);

        // 4. Génération du JWT Token
        return genererReponseLogin(admin);
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants incorrects"));

        if (!passwordEncoder.matches(request.getMotDePasse(), utilisateur.getMotDePasseHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants incorrects");
        }

        if (!Boolean.TRUE.equals(utilisateur.getActif())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte désactivé");
        }

        return genererReponseLogin(utilisateur);
    }

    @Transactional
    public void changerMotDePasse(String email, ChangerMotDePasseRequestDTO request) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.getAncienMotDePasse(), utilisateur.getMotDePasseHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'ancien mot de passe est incorrect");
        }

        utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateur.setDoitChangerMotDePasse(false);
        utilisateurRepository.save(utilisateur);
    }

    private LoginResponseDTO genererReponseLogin(Utilisateur utilisateur) {
        String roleStr = utilisateur.getRole() != null ? utilisateur.getRole().name() : null;
        String idEntrepriseStr = utilisateur.getEntreprise() != null && utilisateur.getEntreprise().getIdEntreprise() != null 
                ? utilisateur.getEntreprise().getIdEntreprise().toString() 
                : null;
        String idUtilisateurStr = utilisateur.getIdUtilisateur() != null 
                ? utilisateur.getIdUtilisateur().toString() 
                : null;

        // Génération du token via la signature à 4 arguments de JwtService
        String token = jwtService.generateToken(
                utilisateur.getEmail(),
                roleStr,
                idEntrepriseStr,
                idUtilisateurStr
        );

        return LoginResponseDTO.builder()
                .token(token)
                .idUtilisateur(utilisateur.getIdUtilisateur())
                .nom(utilisateur.getNom())
                .email(utilisateur.getEmail())
                .role(roleStr)
                .idEntreprise(utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getIdEntreprise() : null)
                .doitChangerMotDePasse(utilisateur.getDoitChangerMotDePasse())
                .build();
    }
}