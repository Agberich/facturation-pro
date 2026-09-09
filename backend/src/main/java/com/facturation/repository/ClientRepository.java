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

    Optional<Client> findByIdClientAndEntrepriseIdEntreprise(UUID idClient, UUID idEntreprise);

    // 1. Uniquement les clients actifs et non supprimés
    List<Client> findByEntrepriseIdEntrepriseAndActifTrueAndDeletedAtIsNull(UUID idEntreprise);

    // 2. Tous les clients (actifs ET inactifs) non supprimés
    List<Client> findByEntrepriseIdEntrepriseAndDeletedAtIsNull(UUID idEntreprise);

    @Modifying
    @Query("UPDATE Client c SET c.actif = false WHERE c.idClient = :idClient AND c.entreprise.idEntreprise = :idEntreprise")
    int desactiverClient(@Param("idClient") UUID idClient, @Param("idEntreprise") UUID idEntreprise);

    @Modifying
    @Query("UPDATE Client c SET c.actif = true WHERE c.idClient = :idClient AND c.entreprise.idEntreprise = :idEntreprise")
    int reactiverClient(@Param("idClient") UUID idClient, @Param("idEntreprise") UUID idEntreprise);
}