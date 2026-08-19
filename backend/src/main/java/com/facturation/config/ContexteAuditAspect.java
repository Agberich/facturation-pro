package com.facturation.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Positionne l'utilisateur courant, son adresse IP et son user-agent dans la
 * session Postgres avant l'exécution des méthodes de service
 * transactionnelles, afin que fn_audit_historique() (V4__Functions.sql)
 * puisse les tracer dans historique_action.
 *
 * L'ordre d'exécution par rapport au proxy @Transactional est garanti par
 * TransactionConfig (@EnableTransactionManagement(order = 0)) : la
 * transaction est déjà ouverte quand cet aspect s'exécute, donc
 * set_config(..., true) (portée locale à la transaction) reste visible
 * pour toutes les requêtes/déclencheurs exécutés ensuite dans la même
 * méthode de service.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class ContexteAuditAspect {

    private final EntityManager entityManager;

    @Before("execution(* com.facturation.service..*(..))")
    public void positionnerContexteCourant() {
        // Utilisation de getIdUtilisateurAsString() pour fournir la représentation textuelle attendue par PostgreSQL
        positionnerParametre("app.current_user_id", ContexteRequete.getIdUtilisateurAsString());
        positionnerParametre("app.current_ip", ContexteRequete.getAdresseIp());
        positionnerParametre("app.current_user_agent", ContexteRequete.getUserAgent());
    }

    private void positionnerParametre(String cle, String valeur) {
        if (valeur != null && !valeur.isBlank()) {
            entityManager.createNativeQuery("SELECT set_config(:cle, :valeur, true)")
                    .setParameter("cle", cle)
                    .setParameter("valeur", valeur)
                    .getSingleResult();
        }
    }
}