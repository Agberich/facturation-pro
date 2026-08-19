package com.facturation.repository;

import com.facturation.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    // Chercher un client spécifique par son ID et l'ID de son entreprise
    Optional<Client> findByIdClientAndEntrepriseIdEntreprise(UUID idClient, UUID idEntreprise);

    // 1. Liste normale : Clients uniquement ACTIFS (actif = true) et NON SUPPRIMÉS (deletedAt = NULL)
    List<Client> findByEntrepriseIdEntrepriseAndActifTrueAndDeletedAtIsNull(UUID idEntreprise);

    // 2. Liste complète : Clients ACTIFS + ARCHIVÉS (actif = true OU false), mais toujours NON SUPPRIMÉS (deletedAt = NULL)
    List<Client> findByEntrepriseIdEntrepriseAndDeletedAtIsNull(UUID idEntreprise);

    // Pour l'archivage/désactivation d'un client spécifique
    @Modifying
    @Query("UPDATE Client c SET c.actif = false WHERE c.idClient = :idClient AND c.entreprise.idEntreprise = :idEntreprise")
    int desactiverClient(@Param("idClient") UUID idClient, @Param("idEntreprise") UUID idEntreprise);

    // Pour la réactivation d'un client spécifique
    @Modifying
    @Query("UPDATE Client c SET c.actif = true WHERE c.idClient = :idClient AND c.entreprise.idEntreprise = :idEntreprise")
    int reactiverClient(@Param("idClient") UUID idClient, @Param("idEntreprise") UUID idEntreprise);
}