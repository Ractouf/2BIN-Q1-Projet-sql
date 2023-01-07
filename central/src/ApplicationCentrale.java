import java.sql.*;
import java.util.Scanner;
public class ApplicationCentrale {
    private String salt = BCrypt.gensalt();
    private Connection conn = null;
    private Scanner scanner = new Scanner(System.in);
    private PreparedStatement ajouterCours;
    private PreparedStatement ajouterEtudiant;
    private PreparedStatement inscrireEtudiantCours;
    private PreparedStatement creerProjet;
    private PreparedStatement creerGroupe;
    private PreparedStatement visualiserCours;
    private PreparedStatement visualiserProjets;
    private PreparedStatement visualiserGroupesProjet;
    private PreparedStatement validerGroupe;
    private PreparedStatement validerGroupes;

    public ApplicationCentrale() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver PostgreSQL manquant !");
            System.exit(1);
        }

        //String url = "jdbc:postgresql://172.24.2.6:5432/dbfrancoisvandeputte";
        String url = "jdbc:postgresql://localhost:5432/postgres";
        try {
            //conn = DriverManager.getConnection(url, "francoisvandeputte", "");
            conn = DriverManager.getConnection(url, "postgres", "F20022002f!");
        } catch (SQLException e) {
            System.out.println("Impossible de joindre le server !");
            System.exit(1);
        }

        try {
            ajouterCours = conn.prepareStatement("SELECT projet.ajouter_cours (?, ?, ?, ?)");
            ajouterEtudiant = conn.prepareStatement("SELECT projet.ajouter_etudiant (?, ?, ?, ?)");
            inscrireEtudiantCours = conn.prepareStatement("SELECT projet.inscrire_etudiant_cours (?, ?)");
            creerProjet = conn.prepareStatement("SELECT projet.creer_projet (?, ?, ?, ?, ?)");
            creerGroupe = conn.prepareStatement("SELECT projet.creer_groupe_projet (?, ?, ?)");
            visualiserCours = conn.prepareStatement("SELECT * FROM projet.visualiser_cours");
            visualiserProjets = conn.prepareStatement("SELECT * FROM projet.visualiser_projets");
            visualiserGroupesProjet = conn.prepareStatement("SELECT * FROM projet.visualiser_groupes_projet WHERE projet = ?");
            validerGroupe = conn.prepareStatement("SELECT * FROM projet.valider_groupe (?, ?)");
            validerGroupes = conn.prepareStatement("SELECT * FROM projet.valider_groupes (?)");
        } catch (SQLException se) {
            System.out.println("Erreur !");
            se.printStackTrace();
            System.exit(1);
        }
    }

    public void start() {
        int numero = 1;

        while (true) {
            System.out.println("1 - Ajouter un cours");
            System.out.println("2 - Ajouter un(e) étudiant(e)");
            System.out.println("3 - Inscrire un(e) étudiant(e) a un cours");
            System.out.println("4 - Créer un projet pour un cours");
            System.out.println("5 - Créer un/des groupe(s) pour un projet");
            System.out.println("6 - Visualiser les cours");
            System.out.println("7 - Visualiser les projets");
            System.out.println("8 - Visualser toutes les compositions de groupe d'un projet");
            System.out.println("9 - Valider un groupe");
            System.out.println("10 - Valider tout les groupes d'un projet");
            System.out.println("---");
            System.out.println("0 - Quitter");

            try {
                numero = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Veuillez entrer un entier\n");
                continue;
            }

            if (numero == 0)
                break;

            if (numero < 0 || numero > 10) {
                System.out.println("Veuillez entrer un entier entre 1 et 10\n");
                continue;
            }

            switch (numero) {
                case 1 -> ajouterCours();
                case 2 -> ajouterEtudiant();
                case 3 -> inscrireEtudiantCours();
                case 4 -> creerProjet();
                case 5 -> creerGroupe();
                case 6 -> visualiserCours();
                case 7 -> visualiserProjets();
                case 8 -> visualiserGroupesProjet();
                case 9 -> validerGroupe();
                case 10 -> validerGroupes();
                default -> {
                }
            }
        }
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        scanner.close();

        System.out.println("Sortie du programme");
    }
    public void  ajouterCours() {
        System.out.println("Ajouter un cours");

        System.out.println("Code du cours: ");
        String codeCours = scanner.nextLine();
        System.out.println("Nom du cours: ");
        String nomCours = scanner.nextLine();

        int credits = 0;
        int bloc = 0;
        try {
            System.out.println("Crédits: ");
            credits = Integer.parseInt(scanner.nextLine());
            System.out.println("Bloc: ");
            bloc = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Veuillez entrer un entier\n");
            ajouterCours();
        }

        try {
            ajouterCours.setString(1, codeCours);
            ajouterCours.setString(2, nomCours);
            ajouterCours.setInt(3, credits);
            ajouterCours.setInt(4, bloc);

            ajouterCours.executeQuery();
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void ajouterEtudiant() {
        System.out.println("Ajouter un(e) étudiant(e)");

        System.out.println("Nom de l'étudiant: ");
        String nomEtudiant = scanner.nextLine();
        System.out.println("Prénom de l'étudiant: ");
        String prenomEtudiant = scanner.nextLine();
        System.out.println("Email vinci de l'étudiant: ");
        String emailVinciEtudiant = scanner.nextLine();
        System.out.println("Mot de passe: ");
        String motDePasseEtudiant = BCrypt.hashpw(scanner.nextLine(), salt);

        try {
            ajouterEtudiant.setString(1, nomEtudiant);
            ajouterEtudiant.setString(2, prenomEtudiant);
            ajouterEtudiant.setString(3, emailVinciEtudiant);
            ajouterEtudiant.setString(4, motDePasseEtudiant);
            ajouterEtudiant.executeQuery();
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void inscrireEtudiantCours() {
        System.out.println("Inscrire un(e) étudiant(e) a un cours");

        System.out.println("Code du cours: ");
        String codeCours = scanner.nextLine();

        System.out.println("Identifiant de l'étudiant: ");
        String emailEtudiant = scanner.nextLine();;

        try {
            inscrireEtudiantCours.setString(1, codeCours);
            inscrireEtudiantCours.setString(2, emailEtudiant);

            inscrireEtudiantCours.executeQuery();
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void creerProjet() {
        System.out.println("Créer un projet pour un cours");

        System.out.println("Identifiant du projet : ");
        String idProjet = scanner.nextLine();
        System.out.println("Nom du projet : ");
        String nomProjet = scanner.nextLine();
        System.out.println("Date de début du projet (YYYY-MM-DD): ");
        Date dateDebut = Date.valueOf(scanner.nextLine());
        System.out.println("Date de fin du projet (YYYY-MM-DD): ");
        Date dateFin = Date.valueOf(scanner.nextLine());
        System.out.println("Code du cours: ");
        String codeCours = scanner.nextLine();

        try {
            creerProjet.setString(1, idProjet);
            creerProjet.setString(2, nomProjet);
            creerProjet.setDate(3, dateDebut);
            creerProjet.setDate(4, dateFin);
            creerProjet.setString(5, codeCours);
            creerProjet.executeQuery();
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void creerGroupe() {
        System.out.println("Créer un/des groupe(s) pour un projet");

        System.out.println("Identifiant du projet : ");
        String idProjet = scanner.nextLine();

        int nombreGroupes = 0;
        int nombreMaxMembres = 0;
        try {
            System.out.println("Nombre de groupes : ");
            nombreGroupes = Integer.parseInt(scanner.nextLine());
            System.out.println("Nombre maximum de membres par groupe : ");
            nombreMaxMembres = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Veuillez entrer un entier\n");
            creerGroupe();
        }

        try {
            creerGroupe.setString(1, idProjet);
            creerGroupe.setInt(2, nombreGroupes);
            creerGroupe.setInt(3, nombreMaxMembres);

            creerGroupe.executeQuery();
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void visualiserCours() {
        System.out.println("Visualiser les cours");

        try (ResultSet rs = visualiserCours.executeQuery()) {
            while (rs.next()) {
                System.out.print(
                        "Code du cours : " + rs.getString(1) + "\n"
                        + "Nom du cours : " + rs.getString(2) + "\n"
                        + "Projets : " + rs.getString(3) + "\n"
                );

                System.out.print("\n");
            }
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void visualiserProjets() {
        System.out.println("Visualiser les projets");

        try (ResultSet rs = visualiserProjets.executeQuery()) {
            while (rs.next()) {
                System.out.print(
                        "Identidiant du projet : " + rs.getString(1) + "\n"
                        + "Nom du projet : " + rs.getString(2) + "\n"
                        + "Code du cours : " + rs.getString(3) + "\n"
                        + "Nombre de groupe : " + rs.getInt(4) + "\n"
                        + "Nombre de groupes complets : " + rs.getInt(5) + "\n"
                        + "Nombre de groupes valides : " + rs.getInt(6) + "\n"
                );

                System.out.print("\n");
            }
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void visualiserGroupesProjet() {
        System.out.println("Visualser toutes les compositions de groupe d'un projet");

        System.out.println("Identifiant du projet: ");
        String idProjet = scanner.nextLine();

        try {
            visualiserGroupesProjet.setString(1, idProjet);
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }

        try (ResultSet rs = visualiserGroupesProjet.executeQuery()) {
            while (rs.next()) {
                System.out.print(
                        "Identifiant du projet : " + rs.getString(1) + "\n"
                        + "Numéro du groupe : " + rs.getInt(2) + "\n"
                        + "Nom : " + rs.getString(3) + "\n"
                        + "Prénom : " + rs.getString(4) + "\n"
                        + "Complet ? " + rs.getBoolean(5) + "\n"
                        + "Validé ? " + rs.getBoolean(6) + "\n"
                );

                System.out.print("\n");
            }
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void validerGroupe() {
        System.out.println("Valider un groupe");

        System.out.println("Identifiant du projet : ");
        String idProjet = scanner.nextLine();

        int numeroGroupe = 0;
        try {
            System.out.println("Numéro du groupe : ");
            numeroGroupe = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Veuillez entrer un entier\n");
            validerGroupes();
        }

        try {
            validerGroupe.setString(1, idProjet);
            validerGroupe.setInt(2, numeroGroupe);

            validerGroupe.executeQuery();
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void validerGroupes() {
        System.out.println("Valider tout les groupes d'un projet");

        System.out.println("Identifiant du projet : ");
        String idProjet = scanner.nextLine();

        try {
            validerGroupes.setString(1, idProjet);

            validerGroupes.executeQuery();
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
}
