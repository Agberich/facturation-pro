package com.facturation.service;

import com.facturation.entity.Client;
import com.facturation.entity.Entreprise;
import com.facturation.entity.Facturation;
import com.facturation.entity.LigneFacturation;
import com.facturation.repository.ClientRepository;
import com.facturation.repository.EntrepriseRepository;
import com.facturation.repository.FacturationRepository;
import com.facturation.repository.LigneFacturationRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacturationService {

    private static final Logger log = LoggerFactory.getLogger(FacturationService.class);

    private final FacturationRepository facturationRepository;
    private final ClientRepository clientRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final LigneFacturationRepository ligneFacturationRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Facturation> listerFacturations(UUID idEntreprise) {
        if (idEntreprise == null) {
            throw new IllegalArgumentException("L'ID de l'entreprise ne peut pas être nul");
        }
        return facturationRepository.findByEntrepriseIdEntrepriseOrderByAnneeDescMoisDesc(idEntreprise);
    }

    @Transactional(readOnly = true)
    public Facturation obtenirFacturation(UUID idFacturation) {
        if (idFacturation == null) {
            throw new IllegalArgumentException("L'ID de la facturation ne peut pas être nul");
        }
        return facturationRepository.findById(idFacturation)
                .orElseThrow(() -> new IllegalArgumentException("Facture introuvable : " + idFacturation));
    }

    @Transactional(readOnly = true)
    public List<LigneFacturation> listerLignes(UUID idFacturation) {
        if (idFacturation == null) {
            throw new IllegalArgumentException("L'ID de la facturation ne peut pas être nul");
        }
        return ligneFacturationRepository.findByFacturationIdFacturationOrderByOrdreAffichageAsc(idFacturation);
    }

    @Transactional
    public Facturation initialiserMoisFacturation(UUID idEntreprise, Integer annee, Integer mois) {
        if (idEntreprise == null || annee == null || mois == null) {
            throw new IllegalArgumentException("L'ID de l'entreprise, l'année et le mois sont obligatoires");
        }

        facturationRepository.findByEntrepriseIdEntrepriseAndAnneeAndMois(idEntreprise, annee, mois)
                .ifPresent(f -> {
                    throw new IllegalStateException("La facturation pour ce mois existe déjà.");
                });

        Entreprise entreprise = entrepriseRepository.findById(idEntreprise)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise introuvable : " + idEntreprise));

        LocalDate debutMois = LocalDate.of(annee, mois, 1);
        LocalDate finMois = debutMois.withDayOfMonth(debutMois.lengthOfMonth());

        Facturation nouvelleFacturation = Facturation.builder()
                .entreprise(entreprise)
                .annee(annee)
                .mois(mois)
                .dateDebutPeriode(debutMois)
                .dateFinPeriode(finMois)
                .statut(Facturation.StatutFacturation.BROUILLON)
                .build();

        Facturation facturationEnregistree = facturationRepository.save(nouvelleFacturation);
        log.info("Facturation initialisée pour le mois {}/{} (Entreprise ID: {})", mois, annee, idEntreprise);
        return facturationEnregistree;
    }

    @Transactional
    public List<LigneFacturation> genererLignesPourFacturation(UUID idFacturation) {
        Facturation facturation = obtenirFacturation(idFacturation);

        if (facturation.getStatut() != Facturation.StatutFacturation.BROUILLON) {
            throw new IllegalStateException(
                    "Les lignes ne peuvent être générées ou recalculées que pour une facturation en brouillon.");
        }

        UUID idEntreprise = facturation.getEntreprise().getIdEntreprise();
        List<Client> clientsActifs = clientRepository
                .findByEntrepriseIdEntrepriseAndActifTrueAndDeletedAtIsNull(idEntreprise);

        int ordre = ligneFacturationRepository
                .findByFacturationIdFacturationOrderByOrdreAffichageAsc(idFacturation)
                .size();

        for (Client client : clientsActifs) {
            LigneFacturation ligneExistante = ligneFacturationRepository
                    .findByFacturationIdFacturationAndClientIdClient(idFacturation, client.getIdClient())
                    .orElse(null);

            if (ligneExistante != null) {
                ligneExistante.setTarifApplique(client.getTarifParDefaut());
                ligneExistante.setDateSortieEffective(client.getDateSortie());
                ligneFacturationRepository.save(ligneExistante);
                recalculerLigne(ligneExistante.getIdLigne());
                continue;
            }

            if (client.getDateEntree() != null && client.getDateEntree().isAfter(facturation.getDateFinPeriode())) {
                continue;
            }
            if (client.getDateSortie() != null && client.getDateSortie().isBefore(facturation.getDateDebutPeriode())) {
                continue;
            }

            LigneFacturation.StatutLigne statutLigne = determinerStatutLigne(client, facturation);

            LigneFacturation ligne = LigneFacturation.builder()
                    .facturation(facturation)
                    .client(client)
                    .dateEntreeEffective(client.getDateEntree() != null ? client.getDateEntree() : facturation.getDateDebutPeriode())
                    .dateSortieEffective(client.getDateSortie())
                    .dernierJourMois(facturation.getDateFinPeriode())
                    .tarifApplique(client.getTarifParDefaut())
                    .statut(statutLigne)
                    .ordreAffichage(++ordre)
                    .build();

            ligne = ligneFacturationRepository.save(ligne);
            recalculerLigne(ligne.getIdLigne());
        }

        log.info("Lignes générées/recalculées pour la facturation ID {}", idFacturation);
        return listerLignes(idFacturation);
    }

    private void recalculerLigne(UUID idLigne) {
        entityManager.flush();
        entityManager.createNativeQuery("SELECT app_facturation.recalculer_ligne_facturation(:idLigne)")
                .setParameter("idLigne", idLigne)
                .getSingleResult();
        entityManager.clear();
    }

    private LigneFacturation.StatutLigne determinerStatutLigne(Client client, Facturation facturation) {
        boolean entreDansLePeriode = client.getDateEntree() != null
                && !client.getDateEntree().isBefore(facturation.getDateDebutPeriode())
                && !client.getDateEntree().isAfter(facturation.getDateFinPeriode());

        boolean sortiDansLePeriode = client.getDateSortie() != null
                && !client.getDateSortie().isBefore(facturation.getDateDebutPeriode())
                && !client.getDateSortie().isAfter(facturation.getDateFinPeriode());

        if (sortiDansLePeriode) {
            return LigneFacturation.StatutLigne.SORTI;
        }
        if (entreDansLePeriode) {
            return LigneFacturation.StatutLigne.NOUVEAU;
        }
        return LigneFacturation.StatutLigne.ACTIF;
    }

    @Transactional
    public Facturation validerFacture(UUID idFacturation) {
        Facturation facture = obtenirFacturation(idFacturation);

        if (Facturation.StatutFacturation.VALIDEE.equals(facture.getStatut())) {
            return facture;
        }

        List<LigneFacturation> lignes = ligneFacturationRepository
                .findByFacturationIdFacturationOrderByOrdreAffichageAsc(idFacturation);
        if (lignes.isEmpty()) {
            throw new IllegalStateException(
                    "Impossible de valider une facturation sans aucune ligne. Générez d'abord les lignes de facturation.");
        }

        // Un numéro n'est généré que la toute première fois qu'une facturation est
        // validée. Si elle a déjà un numéro (cas d'une réouverture pour correction,
        // suivie d'une nouvelle validation), on le conserve : c'est toujours la même
        // facture, pas une nouvelle — seul son contenu a été corrigé.
        if (facture.getNumeroFacture() == null) {
            String nouveauNumero = (String) entityManager.createNativeQuery(
                    "SELECT app_facturation.generer_numero_facture(:idEntreprise, :annee)")
                    .setParameter("idEntreprise", facture.getEntreprise().getIdEntreprise())
                    .setParameter("annee", facture.getAnnee())
                    .getSingleResult();
            facture.setNumeroFacture(nouveauNumero);
            log.info("Facture validée avec le nouveau numéro {} (ID: {})", nouveauNumero, idFacturation);
        } else {
            log.info("Facture revalidée en conservant son numéro {} (ID: {})", facture.getNumeroFacture(), idFacturation);
        }

        facture.setStatut(Facturation.StatutFacturation.VALIDEE);
        facture.setDateValidation(OffsetDateTime.now());

        return facturationRepository.save(facture);
    }

    @Transactional
    public Facturation reouvrirFacture(UUID idFacturation) {
        Facturation facture = obtenirFacturation(idFacturation);

        if (facture.getStatut() != Facturation.StatutFacturation.VALIDEE) {
            throw new IllegalStateException("Seule une facturation validée peut être réouverte.");
        }

        facture.setStatut(Facturation.StatutFacturation.BROUILLON);
        facture.setDateValidation(null);
        facture.setValidePar(null);

        log.info("Facture réouverte en mode brouillon (ID: {})", idFacturation);
        return facturationRepository.save(facture);
    }
}