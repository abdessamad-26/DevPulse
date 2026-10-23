# DevPulse — rapport final de remédiation

**Date :** 2026-10-23
**Branche observée :** `modernize/p1-quality`
**Verdict :** **Non prêt pour la production**

## Synthèse

Le dépôt a été inspecté et les corrections réalisables localement ont été
implémentées sans réinitialiser ni supprimer les changements préexistants.
L’application Java a été alignée sur Java 25 LTS et vérifiée avec succès sur
cette version. Le défaut d’état d’erreur de la page Logs a été corrigé et
couvert par un test. Les dépendances frontend ayant des avis CVE connus ont
été mises à jour vers des versions compatibles corrigées; les audits npm et
GitHub Security Advisories ciblés sont désormais propres.

La vérification PostgreSQL/Flyway, les images Docker, les parcours E2E complets
et l’exécution de la CI hébergée n’ont pas pu être démontrés. Des lacunes
fonctionnelles nécessitant un contrat métier/API — notamment administration
globale des utilisateurs et réglages du compte — restent également hors
implémentation sûre. Ces limites empêchent de déclarer le produit prêt pour la
production.

## Corrections réalisées

- Java 25 LTS est configuré dans `backend/pom.xml`, les étapes build/runtime de
  `backend/Dockerfile`, le workflow CI et le README.
- Les tests d’intégration Java ne se gênent plus mutuellement via le limiteur
  de débit d’authentification lorsqu’ils partagent l’adresse IP locale. Les
  tests dédiés du filtre continuent de vérifier le throttling.
- La page Logs sépare les états loading, erreur, résultat vide et résultat
  renseigné, et permet de réessayer après une erreur.
- Les versions Angular vulnérables ont été alignées sur Angular 22.2.3 et
  Angular Build/CLI 22.2.4. Vite 8.3.5 est forcé également dans l’arbre imbriqué
  d’Angular Build.
- Les dépendances MCP, Vitest et `source-map-js` ont été corrigées dans le
  lockfile. Aucun changement de version majeure n’a été forcé.
- `AUDIT.md`, `PLAN.md`, `CHANGELOG.md` et cette checklist/ce rapport consignent
  les preuves ainsi que les limites constatées.

## Vérifications et résultats réels

| Vérification | Résultat |
| --- | --- |
| Backend, `mvn clean verify` avec Temurin 25.0.4.1 et Maven 3.9.15 temporaires vérifiés par checksum | **Succès — 94 tests, 0 échec, 0 erreur** |
| Frontend, `npm test -- --watch=false` | **Succès — 8 tests, 4 fichiers** |
| Frontend, `npm run build` | **Succès — build de production** |
| `npm audit --audit-level=low` | **Succès — 0 vulnérabilité** |
| Analyse GitHub Security Advisories sur les dépendances précédemment affectées | **Aucun CVE connu dans les coordonnées corrigées** |
| Service Python, `python -m pytest` (baseline initiale; Python non modifié ensuite) | **22 tests passés**, 25 avertissements de dépréciation/compatibilité |
| `docker compose config --quiet` | **Succès — configuration valide** |
| `git diff --check` | **Succès — aucune erreur d’espacement** |
| Construction d’image Docker et validation PostgreSQL/Flyway | **Non exécutées jusqu’au bout — daemon Docker local indisponible** |

Le scan initial avait relevé 14 avis sur les versions concernées de
`@angular/router`, `@modelcontextprotocol/server`, `@vitest/mocker`,
`source-map-js` et Vite. Les versions installées sont respectivement 22.2.3,
2.3.1, 5.0.5, 1.2.2 et 8.3.5. Le scan final ciblé n’a retourné aucune
vulnérabilité connue. Cela ne constitue pas un audit d’intrusion complet.

## Risques et travaux restants

1. **Bloquant release :** construire et exécuter les images dans un
   environnement Docker; valider les migrations Flyway sur PostgreSQL vierge
   et existant.
2. **Bloquant release :** exécuter le workflow CI hébergé et les parcours E2E
   complets, dont les échanges Java-Python et le cycle de session.
3. **Contrat à définir :** administration globale des utilisateurs et
   réglages du compte; ne pas créer de permissions seulement côté UI.
4. **Observabilité :** Prometheus/Grafana/Loki ne sont pas déployés dans la
   configuration vérifiée; confirmer si ces éléments sont requis ou futurs.
5. **Python :** les tests de baseline passent mais 25 avertissements de
   dépendance/runtime demeurent à traiter ou accepter explicitement.
6. **Sécurité opérationnelle :** vérifier secrets et configuration dans
   l’environnement cible, sauvegarde/restauration, révocation des sessions et
   autorisations en exécution.

Les endpoints GET de détail de projets et d’incidents ainsi que l’évaluation
des durées de règles d’alerte existaient déjà; la documentation périmée a été
corrigée. Ils ne sont donc pas présentés comme des fonctionnalités nouvelles.

## État Git et opérations

Le dépôt contenait déjà des modifications et fichiers non suivis avant cette
remédiation. Ils ont été conservés; les fichiers locaux sensibles connus n’ont
pas été consultés. Aucun commit, push, déploiement, migration de production ou
opération destructive n’a été effectué.

Voir [RELEASE_CHECKLIST.md](./RELEASE_CHECKLIST.md) pour les conditions
restantes avant release.
