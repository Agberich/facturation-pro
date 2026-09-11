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

import java.time.LocalDate;
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
            return clientRepository.findByEntrepriseIdEntrepriseAndDeletedAtIsNull(idEntreprise);
        }
        
        return clientRepository.findByEntrepriseIdEntrepriseAndActifTrueAndDeletedAtIsNull(idEntreprise);
    }

    @Transactional(readOnly = true)
    public List<Client> obtenirTousLesClients(UUID idEntreprise) {
        // Par defaut, renvoie tous les clients (actifs + inactifs non supprimes) 
        // pour eviter le filtrage intempestif dans la vue globale de l'UI
        return obtenirTousLesClients(idEntreprise, true);
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
        if (client.getActif() == null) {
            client.setActif(true);
        }
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

        // Copie explicite du statut actif/inactif
        if (clientModifie.getActif() != null) {
            clientExistant.setActif(clientModifie.getActif());
        }

        log.info("Client mis à jour : ID {}, Actif: {}", idClient, clientExistant.getActif());
        return clientRepository.save(clientExistant);
    }

    @Transactional
    public void desactiverClient(UUID idClient, LocalDate dateSortie) {
        Client client = obtenirClientParId(idClient);
        client.setActif(false);
        client.setDeletedAt(null);
        // Si une date de sortie est fournie (depart reel en cours de periode),
        // on la conserve : la facturation en cours pourra proratiser jusqu'a
        // cette date au lieu de retirer le client d'un coup au recalcul.
        if (dateSortie != null) {
            client.setDateSortie(dateSortie);
        }
        clientRepository.save(client);
        log.info("Client désactivé (archivé) : ID {}, dateSortie: {}", idClient, dateSortie);
    }

    @Transactional
    public void reactiverClient(UUID idClient) {
        Client client = obtenirClientParId(idClient);
        client.setActif(true);
        client.setDeletedAt(null);
        clientRepository.save(client);
        log.info("Client réactivé : ID {}", idClient);
    }

    @Transactional
    public void desactiverClientsEnMasse(List<UUID> idsClients) {
        if (idsClients == null || idsClients.isEmpty()) return;
        List<Client> clients = clientRepository.findAllById(idsClients);
        clients.forEach(client -> {
            client.setActif(false);
            client.setDeletedAt(null);
        });
        clientRepository.saveAll(clients);
        log.info("{} clients désactivés en masse", clients.size());
    }

    @Transactional
    public void reactiverClientsEnMasse(List<UUID> idsClients) {
        if (idsClients == null || idsClients.isEmpty()) return;
        List<Client> clients = clientRepository.findAllById(idsClients);
        clients.forEach(client -> {
            client.setActif(true);
            client.setDeletedAt(null);
        });
        clientRepository.saveAll(clients);
        log.info("{} clients réactivés en masse", clients.size());
    }

    @Transactional
    public void archiverClientDefinitivement(UUID idClient) {
        Client client = obtenirClientParId(idClient);
        client.setActif(false);
        client.setDeletedAt(OffsetDateTime.now());
        clientRepository.save(client);
        log.info("Client archivé définitivement (Soft Delete) : ID {}", idClient);
    }
}