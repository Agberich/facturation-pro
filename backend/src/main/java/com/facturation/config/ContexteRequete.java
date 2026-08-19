package com.facturation.config;

import java.util.UUID;

public class ContexteRequete {

    private static final ThreadLocal<String> ID_ENTREPRISE = new ThreadLocal<>();
    private static final ThreadLocal<String> ID_UTILISATEUR = new ThreadLocal<>();
    private static final ThreadLocal<String> ADRESSE_IP = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_AGENT = new ThreadLocal<>();

    // --- ID ENTREPRISE ---
    public static void setIdEntreprise(String id) { 
        ID_ENTREPRISE.set(id); 
    }
    
    // Surcharge pour accepter un UUID direct
    public static void setIdEntreprise(UUID id) { 
        ID_ENTREPRISE.set(id != null ? id.toString() : null); 
    }

    public static String getIdEntrepriseAsString() { 
        return ID_ENTREPRISE.get(); 
    }

    // Retourne un UUID ou null
    public static UUID getIdEntreprise() {
        String id = ID_ENTREPRISE.get();
        return (id != null && !id.isBlank()) ? UUID.fromString(id) : null;
    }

    // --- ID UTILISATEUR ---
    public static void setIdUtilisateur(String id) { 
        ID_UTILISATEUR.set(id); 
    }
    
    // Surcharge pour accepter un UUID direct
    public static void setIdUtilisateur(UUID id) { 
        ID_UTILISATEUR.set(id != null ? id.toString() : null); 
    }

    public static String getIdUtilisateurAsString() { 
        return ID_UTILISATEUR.get(); 
    }

    // Retourne un UUID ou null
    public static UUID getIdUtilisateur() {
        String id = ID_UTILISATEUR.get();
        return (id != null && !id.isBlank()) ? UUID.fromString(id) : null;
    }

    // --- METADATAS ---
    public static void setAdresseIp(String ip) { ADRESSE_IP.set(ip); }
    public static String getAdresseIp() { return ADRESSE_IP.get(); }

    public static void setUserAgent(String userAgent) { USER_AGENT.set(userAgent); }
    public static String getUserAgent() { return USER_AGENT.get(); }

    public static void clear() {
        ID_ENTREPRISE.remove();
        ID_UTILISATEUR.remove();
        ADRESSE_IP.remove();
        USER_AGENT.remove();
    }
}