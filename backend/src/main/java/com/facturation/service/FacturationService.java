package com.facturation.service;

import com.facturation.entity.Client;
import com.facturation.entity.Entreprise;
import com.facturation.entity.Facturation;
import com.facturation.dto.ResumeFacturationDTO;
import com.facturation.entity.LigneFacturation;
import com.facturation.entity.Parametre;
import com.facturation.repository.ClientRepository;
import com.facturation.repository.EntrepriseRepository;
import com.facturation.repository.FacturationRepository;
import com.facturation.repository.LigneFacturationRepository;
import com.facturation.repository.ParametreRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacturationService {

    private static final Logger log = LoggerFactory.getLogger(FacturationService.class);

    private final FacturationRepository facturationRepository;
    private final ClientRepository clientRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final LigneFacturationRepository ligneFacturationRepository;
    private final ParametreRepository parametreRepository;
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
        BigDecimal tarifJournalier = tarifJournalier(idEntreprise);
        List<Client> clientsActifs = clientRepository
                .findByEntrepriseIdEntrepriseAndActifTrueAndDeletedAtIsNull(idEntreprise);

        Set<UUID> idsClientsActifs = clientsActifs.stream()
                .map(Client::getIdClient)
                .collect(Collectors.toSet());

        // Purge des lignes devenues orphelines : un client peut avoir ete
        // desactive (actif=false) depuis la derniere generation. Sans ce
        // nettoyage, sa ligne restait figee avec ses anciennes valeurs au
        // lieu de disparaitre du recalcul.
        List<LigneFacturation> lignesExistantes = ligneFacturationRepository
                .findByFacturationIdFacturationOrderByOrdreAffichageAsc(idFacturation);
        for (LigneFacturation ligne : lignesExistantes) {
            if (!idsClientsActifs.contains(ligne.getClient().getIdClient())) {
                ligneFacturationRepository.delete(ligne);
            }
        }

        int ordre = ligneFacturationRepository
                .findByFacturationIdFacturationOrderByOrdreAffichageAsc(idFacturation)
                .size();

        for (Client client : clientsActifs) {
            LigneFacturation ligneExistante = ligneFacturationRepository
                    .findByFacturationIdFacturationAndClientIdClient(idFacturation, client.getIdClient())
                    .orElse(null);

            if (ligneExistante != null) {
                ligneExistante.setTarifApplique(tarifJournalier);
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
                    .tarifApplique(tarifJournalier)
                    .statut(statutLigne)
                    .ordreAffichage(++ordre)
                    .build();

            ligne = ligneFacturationRepository.save(ligne);
            recalculerLigne(ligne.getIdLigne());
        }

        // HT et TVA se calculent sur le TOTAL de la facture (règle du modèle) : on répartit les
        // centimes entre les lignes pour que leur somme soit exactement égale au total.
        repartirMontants(idFacturation);

        log.info("Lignes générées/recalculées pour la facturation ID {}", idFacturation);
        return listerLignes(idFacturation);
    }

    private void repartirMontants(UUID idFacturation) {
        entityManager.flush();
        entityManager.createNativeQuery("SELECT app_facturation.repartir_montants_facturation(:idFacturation)")
                .setParameter("idFacturation", idFacturation)
                .getSingleResult();
        entityManager.clear();
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

        // Déjà validée (ou même payée) : on ne touche à rien. Sans cette garde,
        // « valider » une facture payée la ferait repasser en « Validée ».
        if (Facturation.StatutFacturation.VALIDEE.equals(facture.getStatut())
                || Facturation.StatutFacturation.PAYEE.equals(facture.getStatut())) {
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
                    "SELECT app_facturation.generer_numero_facture_mensuel(:idEntreprise, :annee, :mois)")
                    .setParameter("idEntreprise", facture.getEntreprise().getIdEntreprise())
                    .setParameter("annee", facture.getAnnee())
                    .setParameter("mois", facture.getMois())
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

        if (facture.getStatut() == Facturation.StatutFacturation.PAYEE) {
            throw new IllegalStateException(
                    "Cette facturation est payée : annulez d'abord le paiement avant de la réouvrir.");
        }
        if (facture.getStatut() != Facturation.StatutFacturation.VALIDEE) {
            throw new IllegalStateException("Seule une facturation validée peut être réouverte.");
        }

        facture.setStatut(Facturation.StatutFacturation.BROUILLON);
        facture.setDateValidation(null);
        facture.setValidePar(null);

        log.info("Facture réouverte en mode brouillon (ID: {})", idFacturation);
        return facturationRepository.save(facture);
    }

    @Transactional
    public Facturation marquerCommePayee(UUID idFacturation) {
        Facturation facture = obtenirFacturation(idFacturation);

        if (facture.getStatut() == Facturation.StatutFacturation.PAYEE) {
            return facture;
        }
        if (facture.getStatut() != Facturation.StatutFacturation.VALIDEE) {
            throw new IllegalStateException("Seule une facturation validée peut être marquée comme payée.");
        }

        facture.setStatut(Facturation.StatutFacturation.PAYEE);
        facture.setDatePaiement(OffsetDateTime.now());

        log.info("Facture marquée comme payée (ID: {})", idFacturation);
        return facturationRepository.save(facture);
    }

    @Transactional
    public Facturation annulerPaiement(UUID idFacturation) {
        Facturation facture = obtenirFacturation(idFacturation);

        if (facture.getStatut() != Facturation.StatutFacturation.PAYEE) {
            throw new IllegalStateException("Seule une facturation payée peut voir son paiement annulé.");
        }

        facture.setStatut(Facturation.StatutFacturation.VALIDEE);
        facture.setDatePaiement(null);

        log.info("Paiement annulé, facture repassée en « Validée » (ID: {})", idFacturation);
        return facturationRepository.save(facture);
    }

    /** Nombre de personnes et totaux de chaque facturation de l'entreprise (tableau de bord). */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<ResumeFacturationDTO> listerResumes(UUID idEntreprise) {
        if (idEntreprise == null) {
            throw new IllegalArgumentException("L'ID de l'entreprise ne peut pas être nul");
        }
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT id_facturation, total_clients, total_ht, total_tva, total_ttc "
                        + "FROM app_facturation.vw_resume_facturation WHERE id_entreprise = :idEntreprise")
                .setParameter("idEntreprise", idEntreprise)
                .getResultList();

        return rows.stream()
                .map(r -> ResumeFacturationDTO.builder()
                        .idFacturation((UUID) r[0])
                        .nbPersonnes(((Number) r[1]).longValue())
                        .totalHt((BigDecimal) r[2])
                        .totalTva((BigDecimal) r[3])
                        .totalTtc((BigDecimal) r[4])
                        .build())
                .collect(Collectors.toList());
    }

    /** Tarif journalier unique des paramètres (79,92 si aucun paramètre n'existe encore). */
    private BigDecimal tarifJournalier(UUID idEntreprise) {
        return parametreRepository.findByEntrepriseIdEntreprise(idEntreprise)
                .map(Parametre::getTarifJournalier)
                .orElse(Parametre.TARIF_JOURNALIER_PAR_DEFAUT);
    }
}
