import java.sql.*;
import java.util.Scanner;

public class ApplicationEtudiante {
    Connection conn = null;
    Scanner scanner = new Scanner(System.in);
    int idEtudiantConnecte = 0;
    PreparedStatement getEtudiant;
    private PreparedStatement visualiserCoursEtudiant;
    private PreparedStatement inscrireEtudiantGroupe;
    private PreparedStatement retirerEtudiantGroupe;
    private PreparedStatement visualiserProjetsEtudiant;
    private PreparedStatement visualiserProjetsSansGroupeEtudiant;
    private PreparedStatement visualiserCompositionGroupesIncomplets;

    public ApplicationEtudiante() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver PostgreSQL manquant !");
            System.exit(1);
        }

         String url = "jdbc:postgresql://172.24.2.6:5432/dbfrancoisvandeputte";
        //String url = "jdbc:postgresql://localhost:5432/postgres";
        try {
            conn = DriverManager.getConnection(url, "damienlapinski", "");
            //conn = DriverManager.getConnection(url, "postgres", "");
        } catch (SQLException e) {
            System.out.println("Impossible de joindre le server !");
            System.exit(1);
        }
        try {
            getEtudiant = conn.prepareStatement("SELECT id_etudiant, mdp FROM projet.etudiants WHERE email_vinci = ?");

            visualiserCoursEtudiant = conn.prepareStatement("SELECT * FROM projet.visualiser_cours_etudiant WHERE etudiant = ?");
            inscrireEtudiantGroupe = conn.prepareStatement("SELECT projet.inscrire_etudiant_groupe (?, ?, ?)");
            retirerEtudiantGroupe = conn.prepareStatement("SELECT projet.retirer_etudiant_groupe (?, ?)");
            visualiserProjetsEtudiant = conn.prepareStatement("SELECT * FROM projet.visualiser_projets_etudiant WHERE etudiant = ?");
            visualiserProjetsSansGroupeEtudiant = conn.prepareStatement("SELECT * FROM projet.visualiser_projets_sans_groupe_etudiant WHERE etudiant = ?");
            visualiserCompositionGroupesIncomplets = conn.prepareStatement("SELECT * FROM projet.visualiser_composition_groupes_incomplets WHERE projet = ?");
        } catch (SQLException se) {
            System.out.println("Erreur !");
            se.printStackTrace();
            System.exit(1);
        }
    }

    public void start() {
        boolean connected = false;
        do {
            System.out.println("Email vinci:");
            String emailVinci = scanner.nextLine();
            System.out.println("Mot de passe:");
            String motDePasse = scanner.nextLine();

            try {
                getEtudiant.setString(1, emailVinci);
                ResultSet rs = getEtudiant.executeQuery();

                while (rs.next()) {
                    if (BCrypt.checkpw(motDePasse, rs.getString(2))) {
                        idEtudiantConnecte = rs.getInt(1);
                        connected = true;
                        break;
                    }
                }
            } catch (SQLException se) {
                System.out.println(se.getMessage());
            }
        } while (!connected);

        int numero;
        do {
            System.out.println("1 - Visualiser les cours");
            System.out.println("2 - Se rajouter dans un groupe");
            System.out.println("3 - Se retirer d'un groupe");
            System.out.println("4 - Visualiser les projets des cours auxquels il est inscrit");
            System.out.println("5 - Visualiser les projets sans groupe");
            System.out.println("6 - Visualiser toutes les compositions de groupes incomplets d’un projet");

            numero = Integer.parseInt(scanner.nextLine());

            switch (numero) {
                case 1 -> visualiserCoursEtudiant();
                case 2 -> inscrireEtudiantGroupe();
                case 3 -> retirerEtudiantGroupe();
                case 4 -> visualiserProjetsEtudiant();
                case 5 -> visualiserProjetsSansGroupeEtudiant();
                case 6 -> visualiserCompositionGroupesIncomplets();
                default -> {
                }
            }
        } while (numero > 0 && numero < 11);
    }

    public void visualiserCoursEtudiant() {
        System.out.println("Visualiser les cours");

        try {
            visualiserCoursEtudiant.setInt(1, idEtudiantConnecte);
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }

        try (ResultSet rs = visualiserCoursEtudiant.executeQuery()) {
            while (rs.next()) {
                System.out.print(
                    "Code du cours : " + rs.getString(2) + "\n"
                    + "Nom du cours : " + rs.getString(3) + "\n"
                    + "Projets : " + rs.getString(4) + "\n"
                );

                System.out.print("\n");
            }
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void inscrireEtudiantGroupe() {
        System.out.println("Se rajouter dans un groupe");

        System.out.println("Identifiant du projet: ");
        String idProjet = scanner.nextLine();
        System.out.println("Numero du groupe: ");
        int numeroGroupe = Integer.parseInt(scanner.nextLine());


        try {
            inscrireEtudiantGroupe.setString(1, idProjet);
            inscrireEtudiantGroupe.setInt(2, numeroGroupe);
            inscrireEtudiantGroupe.setInt(3, idEtudiantConnecte);

            inscrireEtudiantGroupe.executeQuery();
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void retirerEtudiantGroupe() {
        System.out.println("Se retirer d'un groupe");

        System.out.println("Identifiant du projet: ");
        String idProjet = scanner.nextLine();

        try {
            retirerEtudiantGroupe.setString(1, idProjet);
            retirerEtudiantGroupe.setInt(2, idEtudiantConnecte);

            retirerEtudiantGroupe.executeQuery();
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void visualiserProjetsEtudiant() {
        System.out.println("Visualiser les projets des cours auxquels il est inscrit");

        try {
            visualiserProjetsEtudiant.setInt(1, idEtudiantConnecte);
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }

        try (ResultSet rs = visualiserProjetsEtudiant.executeQuery()) {
            while (rs.next()) {
                System.out.print(
                    "Identifiant du projet : " + rs.getString(2) + "\n"
                    + "Nom du projet : " + rs.getString(3) + "\n"
                    + "Code du cours : " + rs.getString(4) + "\n"
                    + "Numéro du groupe : " + rs.getInt(5) + "\n"
                );

                if (rs.getInt(5) == 0) {
                    System.out.println("Numéro du groupe : " + null);
                }

                System.out.print("\n");
            }
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void visualiserProjetsSansGroupeEtudiant() {
        System.out.println("Visualiser les projets sans groupe");

        try {
            visualiserProjetsSansGroupeEtudiant.setInt(1, idEtudiantConnecte);
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }

        try (ResultSet rs = visualiserProjetsSansGroupeEtudiant.executeQuery()) {
            while (rs.next()) {
                System.out.print(
                    "Identifiant du projet : " + rs.getString(2) + "\n"
                    + "Nom du projet : " + rs.getString(3) + "\n"
                    + "Code du cours : " + rs.getString(4) + "\n"
                    + "Date de début : " + rs.getDate(5) + "\n"
                    + "Date de fin : " + rs.getDate(6) + "\n"
                );

                System.out.print("\n");
            }
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
    public void visualiserCompositionGroupesIncomplets() {
        System.out.println("Visualiser toutes les compositions de groupes incomplets d’un projet");

        System.out.println("Identifiant du projet: ");
        String idProjet = scanner.nextLine();

        try {
            visualiserCompositionGroupesIncomplets.setString(1, idProjet);
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }

        try (ResultSet rs = visualiserCompositionGroupesIncomplets.executeQuery()) {
            while (rs.next()) {
                System.out.print(
                    "Numéro du groupe : " + rs.getInt(2) + "\n"
                    + "Nom de l'étudiant : " + rs.getString(3) + "\n"
                    + "Prénom de l'étudiant : " + rs.getString(4) + "\n"
                    + "Nombre places : " + rs.getInt(5) + "\n"
                );

                System.out.print("\n");
            }
        } catch (SQLException se) {
            System.out.println(se.getMessage());
        }
    }
}
