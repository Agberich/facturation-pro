package com.facturation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Fixe explicitement la priorité (order = 0, la plus haute) de l'aspect
 * transactionnel Spring par rapport aux autres @Aspect de l'application
 * (voir ContexteAuditAspect).
 *
 * Sans cela, l'ordre entre le proxy @Transactional et ContexteAuditAspect
 * n'est pas garanti : ContexteAuditAspect positionne
 * app.current_user_id via set_config(..., true) (portée locale à la
 * transaction), et si cet appel s'exécute AVANT l'ouverture de la
 * transaction Spring (ou dans une connexion différente), le trigger SQL
 * fn_audit_historique ne retrouvera jamais l'utilisateur courant, faussant
 * silencieusement l'audit.
 *
 * En donnant à la gestion transactionnelle la priorité la plus haute
 * (order = 0), le proxy @Transactional devient la couche la plus externe :
 * la transaction est déjà ouverte lorsque ContexteAuditAspect s'exécute,
 * qui reste donc à l'intérieur de la même transaction / connexion.
 */
@Configuration
@EnableTransactionManagement(order = 0)
public class TransactionConfig {
}
