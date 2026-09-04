package com.facturation.repository;

import com.facturation.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    List<Utilisateur> findByEntrepriseIdEntrepriseOrderByNomAsc(UUID idEntreprise);

    List<Utilisateur> findByEntrepriseIdEntrepriseAndActifTrueOrderByNomAsc(UUID idEntreprise);

    List<Utilisateur> findByEntrepriseIdEntrepriseAndActifFalseOrderByNomAsc(UUID idEntreprise);

    Optional<Utilisateur> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdUtilisateurNot(String email, UUID idUtilisateur);

    List<Utilisateur> findByEntrepriseIdEntrepriseAndDeletedAtIsNullOrderByNomAsc(
        UUID idEntreprise
    );

    // Comptage des administrateurs actifs et non supprimés dans une entreprise hors l'utilisateur ciblé
    long countByEntrepriseIdEntrepriseAndRoleAndActifTrueAndDeletedAtIsNullAndIdUtilisateurNot(
        UUID idEntreprise,
        Utilisateur.RoleUtilisateur role,
        UUID idUtilisateur
    );
    // Recherche s'il existe déjà au moins un utilisateur en base
    boolean existsByDeletedAtIsNull();
}