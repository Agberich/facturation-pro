package com.facturation.repository;

import com.facturation.entity.Facturation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacturationRepository extends JpaRepository<Facturation, UUID> {
    
    // Vérifie si une facturation existe déjà pour un mois et une année donnés
    Optional<Facturation> findByEntrepriseIdEntrepriseAndAnneeAndMois(UUID idEntreprise, Integer annee, Integer mois);

    // Liste les facturations d'une entreprise, les plus récentes en premier
    List<Facturation> findByEntrepriseIdEntrepriseOrderByAnneeDescMoisDesc(UUID idEntreprise);
}