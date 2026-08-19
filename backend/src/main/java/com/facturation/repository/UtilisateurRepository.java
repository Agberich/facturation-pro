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

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdUtilisateurNot(String email, UUID idUtilisateur);
    List<Utilisateur> findByEntrepriseIdEntrepriseAndDeletedAtIsNullOrderByNomAsc(
        UUID idEntreprise);
}