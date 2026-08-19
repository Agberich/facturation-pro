package com.facturation.service;

import com.facturation.entity.Client;
import com.facturation.entity.Entreprise;
import com.facturation.repository.ClientRepository;
import com.facturation.repository.EntrepriseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    private final ClientRepository clientRepository;
    private final EntrepriseRepository entrepriseRepository;

    @Transactional(readOnly = true)
    public List<Client> obtenirTousLesClients(UUID idEntreprise, boolean includeInactive) {
        if (idEntreprise == null) {
            throw new IllegalArgumentException("L'ID de l'entreprise ne peut pas être nul");
        }
        
        if (includeInactive) {
            // Renvoie les actifs (actif=true) ET les inactifs/archivés (actif=false) 
            // tant que deletedAt est NULL
            return clientRepository.findByEntrepriseIdEntrepriseAndDeletedAtIsNull(idEntreprise);
        }
        
        // Renvoie uniquement les clients actifs (actif=true et deletedAt IS NULL)
        return clientRepository.findByEntrepriseIdEntrepriseAndActifTrueAndDeletedAtIsNull(idEntreprise);
    }

    @Transactional(readOnly = true)
    public List<Client> obtenirTousLesClients(UUID idEntreprise) {
        return obtenirTousLesClients(idEntreprise, false);
    }

    @Transactional(readOnly = true)
    public Client obtenirClientParId(UUID idClient) {
        if (idClient == null) {
            throw new IllegalArgumentException("L'ID du client ne peut pas être nul");
        }
        return clientRepository.findById(idClient)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable avec l'ID : " + idClient));
    }

    @Transactional
    public Client ajouterClient(UUID idEntreprise, Client client) {
        if (idEntreprise == null) {
            throw new IllegalArgumentException("L'ID de l'entreprise ne peut pas être nul");
        }
        if (client == null) {
            throw new IllegalArgumentException("Les informations du client ne peuvent pas être nulles");
        }

        Entreprise entreprise = entrepriseRepository.findById(idEntreprise)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise introuvable avec l'ID : " + idEntreprise));

        client.setEntreprise(entreprise);
        client.setActif(true);
        client.setDeletedAt(null);
        
        Client clientEnregistre = clientRepository.save(client);
        log.info("Nouveau client créé : {} {} (ID: {})", clientEnregistre.getPrenom(), clientEnregistre.getNom(), clientEnregistre.getIdClient());
        return clientEnregistre;
    }

    @Transactional
    public Client modifierClient(UUID idClient, Client clientModifie) {
        if (idClient == null || clientModifie == null) {
            throw new IllegalArgumentException("Les arguments ne peuvent pas être nuls");
        }
        Client clientExistant = obtenirClientParId(idClient);

        clientExistant.setNom(clientModifie.getNom());
        clientExistant.setPrenom(clientModifie.getPrenom());
        clientExistant.setDateNaissance(clientModifie.getDateNaissance());
        clientExistant.setDateEntree(clientModifie.getDateEntree());
        clientExistant.setDateSortie(clientModifie.getDateSortie());
        clientExistant.setTarifParDefaut(clientModifie.getTarifParDefaut());
        clientExistant.setCommentaire(clientModifie.getCommentaire());

        log.info("Client mis à jour : ID {}", idClient);
        return clientRepository.save(clientExistant);
    }

    /**
     * ARCHIVAGE / DESACTIVATION
     * On passe simplement 'actif' à false.
     * On garde 'deletedAt' à NULL pour qu'il puisse réapparaître quand on coche "Afficher les archivés".
     */
    @Transactional
    public void desactiverClient(UUID idClient) {
        Client client = obtenirClientParId(idClient);
        client.setActif(false);
        client.setDeletedAt(null); // <-- CORRECTION : reste NULL !
        clientRepository.save(client);
        log.info("Client désactivé (archivé) : ID {}", idClient);
    }

    /**
     * REACTIVATION
     * On repasse 'actif' à true.
     */
    @Transactional
    public void reactiverClient(UUID idClient) {
        Client client = obtenirClientParId(idClient);
        client.setActif(true);
        client.setDeletedAt(null);
        clientRepository.save(client);
        log.info("Client réactivé : ID {}", idClient);
    }

    /**
     * SUPPRESSION DEFINITIVE (Soft Delete) - Optionnel
     * Si un jour tu veux VRAIMENT supprimer un client de la vue utilisateur.
     */
    @Transactional
    public void supprimerClientDefinitivement(UUID idClient) {
        Client client = obtenirClientParId(idClient);
        client.setActif(false);
        client.setDeletedAt(OffsetDateTime.now()); // Ici 'deletedAt' sert à la suppression réelle
        clientRepository.save(client);
        log.info("Client supprimé définitivement : ID {}", idClient);
    }
}