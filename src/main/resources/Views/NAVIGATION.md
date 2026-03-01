# Navigation et structure FXML – CampusLink

## Navbar commune
- **`Views/NavBar.fxml`** + **`NavBarController`** : barre de navigation unique incluse dans toutes les pages principales.
- **Inclusion** : `<top><fx:include source="NavBar.fxml"/></top>` dans Student, Publication, Create_Publication, service, Create_Service, ProfileView, TutorDashboardView.
- **Menu** : selon le type d’utilisateur (AppSession), affichage soit **Étudiant** (Rechercher des services, Publications, Mon profil), soit **Tuteur** (Tableau de bord, Mes services, Avis reçus, Mon profil). Déconnexion et logo (retour accueil) communs.

## Convention des chemins
- Tous les FXML sont sous **`/Views/`** (pas `/fxml/`).
- Sous-dossier **`Views/Reviews/`** pour les vues Avis (Admin, Tuteur, Étudiant).

## Fichiers FXML (structure unifiée)
- **Racine** : `BorderPane` pour les pages principales (sauf Login/Signup en `StackPane`).
- **Taille scène** : `prefWidth="1400"` `prefHeight="900"` pour toutes les vues principales.
- **Styles** : `stylesheets="/Styles/..."` (chemin absolu depuis la racine des resources). Pages avec navbar incluent `/Styles/student.css` pour le style de la barre.

## Flux de navigation

| Depuis | Action | Vers (FXML) |
|--------|--------|-------------|
| **Login** | Admin | `/Views/AdminDashboard.fxml` |
| **Login** | Étudiant/Prestataire | `/Views/Student.fxml` (Rechercher des services) |
| **Login** | Inscription | `/Views/Signup.fxml` |
| **Login** | Mot de passe oublié | `/Views/ForgotPassword.fxml` |
| **Signup** | Code vérification | `/Views/VerifyCode.fxml` |
| **Signup** | Retour | `/Views/Login.fxml` |
| **ForgotPassword** | Envoyer code → | `/Views/VerifyCode.fxml` |
| **VerifyCode** | Succès → | `/Views/ResetPassword.fxml` |
| **ResetPassword** | Succès → | `/Views/Login.fxml` |
| **AdminDashboard** | Profil | `/Views/AdminProfile.fxml` |
| **AdminDashboard** | Avis | `/Views/Reviews/AdminReviews.fxml` |
| **AdminDashboard** | Déconnexion | `/Views/Login.fxml` |
| **AdminProfile** | Retour | `/Views/AdminDashboard.fxml` |
| **ProfileView** (étudiant) | Déconnexion | `/Views/Login.fxml` |
| **ProfileView** | Avis tuteur | `/Views/Reviews/TutorReviews.fxml` |
| **ProfileView** | Avis étudiant | `/Views/Reviews/StudentReviews.fxml` |
| **Student** (étudiant) | Services | `/Views/service.fxml` |
| **Student** | Publications | `/Views/Publication.fxml` |
| **Publication** | Créer publication | `/Views/Create_Publication.fxml` |
| **Publication** | Retour / Services | `/Views/Student.fxml` |
| **Create_Publication** | Retour | `/Views/Publication.fxml` |
| **service** (tuteur) | Créer service | `/Views/Create_Service.fxml` |
| **Create_Service** | Retour | `/Views/service.fxml` |
| **TutorDashboardView** | Avis reçus | `/Views/Reviews/TutorReviews.fxml` |
| **TutorReviews** | Tableau de bord | `/Views/TutorDashboardView.fxml` |
| **AdminReviews** | Retour (même vue) | `/Views/Reviews/AdminReviews.fxml` |

## Point d’entrée
- **MainFx** (`org.example.campusLink.mains.MainFx`) charge **`/Views/Login.fxml`** au démarrage.
