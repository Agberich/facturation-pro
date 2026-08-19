package com.facturation.repository;

import com.facturation.entity.LigneFacturation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LigneFacturationRepository extends JpaRepository<LigneFacturation, UUID> {

    // JOIN FETCH obligatoire : "client" est LAZY et Hibernate6Module (FORCE_LAZY_LOADING=false)
    // serialise un proxy non initialise en "null" au lieu de le charger -> sans ce fetch,
    // l'API renvoyait client:null des qu'on rouvrait une facturation dans une nouvelle requete,
    // ce qui faisait planter le frontend (l.client.nom sur null) et affichait une page blanche.
    @Query("SELECT l FROM LigneFacturation l JOIN FETCH l.client " +
           "WHERE l.facturation.idFacturation = :idFacturation ORDER BY l.ordreAffichage ASC")
    List<LigneFacturation> findByFacturationIdFacturationOrderByOrdreAffichageAsc(@Param("idFacturation") UUID idFacturation);

    boolean existsByFacturationIdFacturationAndClientIdClient(UUID idFacturation, UUID idClient);
    Optional<LigneFacturation> findByFacturationIdFacturationAndClientIdClient(UUID idFacturation, UUID idClient);
}
