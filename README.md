# CampusLink

CampusLink est une plateforme destinée aux étudiants, conçue pour faciliter la gestion d’un marché et de services au sein d’un environnement universitaire.

L’objectif principal du projet est de centraliser les échanges de services entre étudiants, tout en intégrant un système de réservation, de paiement, d’évaluation et de gestion des factures.

---

## Présentation du projet

CampusLink vise à créer un écosystème numérique universitaire permettant aux étudiants de :

- Publier et consulter des services proposés par d'autres étudiants  
- Effectuer et gérer des réservations  
- Suivre leurs paiements  
- Consulter et gérer leurs factures  
- Évaluer les services utilisés  
- Rechercher et filtrer efficacement les activités  

La plateforme favorise la collaboration, la transparence et la confiance au sein de la communauté étudiante.

---

## Fonctionnalités principales

### Gestion des services
- Publication de services par les étudiants  
- Consultation et recherche de services disponibles  
- Filtrage dynamique des résultats  

### Gestion des réservations
- Réservation de services  
- Consultation de l’historique des réservations  
- Gestion des réservations en cours  

### Système de paiement
- Enregistrement et suivi des paiements  
- Historique des transactions  
- Génération automatique de factures  

### Gestion des factures
- Consultation de la liste des factures  
- Recherche par identifiant ou détails  
- Filtrage par date  
- Aperçu détaillé des factures via une fenêtre modale  
- Suppression avec confirmation  

### Gestion des évaluations
- Attribution d’une note à un service après utilisation  
- Ajout de commentaires  
- Consultation des évaluations d’un service  
- Amélioration de la transparence et de la qualité des prestations  

---

## Technologies utilisées

- Java  
- JavaFX  
- FXML  
- CSS (JavaFX)  
- MySQL  
- JDBC  
- Scene Builder  

---

## Architecture du projet

Le projet suit une architecture en couches afin de garantir une séparation claire des responsabilités :

```
campusLink/
│
├── controllers/
├── entities/
├── services/
├── View/
└── Style/
```

- **entities** : Modèles de données  
- **services** : Logique métier et interaction avec la base de données  
- **controllers** : Gestion des interactions utilisateur  
- **View** : Interfaces graphiques (FXML)  
- **Style** : Fichiers CSS  

---

## Objectifs pédagogiques

Ce projet a été réalisé dans un cadre académique afin de mettre en pratique :

- La conception d’applications JavaFX  
- L’architecture MVC  
- L’intégration d’une base de données relationnelle  
- La gestion des transactions et évaluations  
- L’amélioration de l’expérience utilisateur  
- La structuration d’un projet logiciel  

---

## Installation et exécution

1. Cloner le dépôt :

```bash
git clone https://github.com/votre-utilisateur/campuslink.git
```

2. Ouvrir le projet dans IntelliJ IDEA ou Eclipse  

3. Configurer :
   - Le SDK Java  
   - Le SDK JavaFX  
   - La connexion à la base de données  

4. Lancer l’application via la classe principale.

---

## Licence

Projet réalisé dans un cadre académique à des fins éducatives.
