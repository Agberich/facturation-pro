package com.facturation.service;

import com.facturation.dto.UtilisateurRequest;
import com.facturation.dto.UtilisateurResponse;
import com.facturation.entity.Entreprise;
import com.facturation.entity.Utilisateur;
import com.facturation.repository.EntrepriseRepository;
import com.facturation.repository.UtilisateurRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UtilisateurResponse> listerParEntreprise(UUID idEntreprise) {
        return utilisateurRepository.findByEntrepriseIdEntrepriseAndDeletedAtIsNullOrderByNomAsc(idEntreprise)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse obtenirParId(UUID id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'ID : " + id));
        return mapToResponse(utilisateur);
    }

    @Transactional
    public UtilisateurResponse creer(UUID idEntreprise, UtilisateurRequest request) {
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur existe déjà avec cet email : " + request.getEmail());
        }

        Entreprise entreprise = entrepriseRepository.findById(idEntreprise)
                .orElseThrow(() -> new EntityNotFoundException("Entreprise non trouvée avec l'ID : " + idEntreprise));

        Utilisateur utilisateur = Utilisateur.builder()
                .nom(request.getNom())
                .email(request.getEmail())
                .motDePasseHash(passwordEncoder.encode(request.getMotDePasse()))
                .role(request.getRole() != null ? request.getRole() : Utilisateur.RoleUtilisateur.COMPTABLE)
                .actif(request.getActif() != null ? request.getActif() : true)
                .doitChangerMotDePasse(true) // Force le changement de mot de passe à la création
                .entreprise(entreprise)
                .build();

        Utilisateur utilisateurSauvegarde = utilisateurRepository.save(utilisateur);
        return mapToResponse(utilisateurSauvegarde);
    }

    @Transactional
    public UtilisateurResponse modifier(UUID id, UtilisateurRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'ID : " + id));

        if (!utilisateur.getEmail().equals(request.getEmail()) && utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un autre utilisateur utilise déjà cet email : " + request.getEmail());
        }

        utilisateur.setNom(request.getNom());
        utilisateur.setEmail(request.getEmail());

        if (request.getMotDePasse() != null && !request.getMotDePasse().isBlank()) {
            utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getMotDePasse()));
            utilisateur.setDoitChangerMotDePasse(true); // Redemande un changement si le mot de passe est réinitialisé
        }

        if (request.getRole() != null) {
            utilisateur.setRole(request.getRole());
        }

        if (request.getActif() != null) {
            utilisateur.setActif(request.getActif());
        }

        Utilisateur utilisateurMisAJour = utilisateurRepository.save(utilisateur);
        return mapToResponse(utilisateurMisAJour);
    }

    @Transactional
    public void desactiver(UUID id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'ID : " + id));

        verifierDernierAdministrateur(utilisateur);

        utilisateur.setActif(false);
        utilisateurRepository.save(utilisateur);
    }

    @Transactional
    public void reactiver(UUID id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'ID : " + id));

        if (utilisateur.getDeletedAt() != null) {
            throw new IllegalStateException("Impossible de réactiver un utilisateur supprimé");
        }

        utilisateur.setActif(true);
        utilisateurRepository.save(utilisateur);
    }

    @Transactional
    public void supprimer(UUID id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'ID : " + id));

        verifierDernierAdministrateur(utilisateur);

        utilisateur.setDeletedAt(OffsetDateTime.now());
        utilisateur.setActif(false);
        utilisateurRepository.save(utilisateur);
    }

    private void verifierDernierAdministrateur(Utilisateur utilisateur) {
        if (Utilisateur.RoleUtilisateur.ADMIN.equals(utilisateur.getRole())) {
            
            UUID idEntreprise = utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getIdEntreprise() : null;

            if (idEntreprise != null) {
                long nbAdminsRestants = utilisateurRepository
                        .countByEntrepriseIdEntrepriseAndRoleAndActifTrueAndDeletedAtIsNullAndIdUtilisateurNot(
                                idEntreprise,
                                Utilisateur.RoleUtilisateur.ADMIN,
                                utilisateur.getIdUtilisateur()
                        );

                if (nbAdminsRestants == 0) {
                    throw new IllegalStateException("Impossible de désactiver ou supprimer le dernier administrateur de l'entreprise.");
                }
            }
        }
    }

   private UtilisateurResponse mapToResponse(Utilisateur utilisateur) {
    return UtilisateurResponse.builder()
            .idUtilisateur(utilisateur.getIdUtilisateur())
            .nom(utilisateur.getNom())
            .prenom(utilisateur.getPrenom())
            .telephone(utilisateur.getTelephone())
            .email(utilisateur.getEmail())
            .role(utilisateur.getRole())
            .actif(utilisateur.getActif())
            .doitChangerMotDePasse(utilisateur.getDoitChangerMotDePasse())
            .idEntreprise(utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getIdEntreprise() : null)
            .derniereConnexion(utilisateur.getDerniereConnexion())
            .createdAt(utilisateur.getCreatedAt())
            .build();
}
}