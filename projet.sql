DROP SCHEMA IF EXISTS projet CASCADE ;
CREATE SCHEMA projet;

CREATE TABLE projet.cours (
    code_cours CHAR(8) PRIMARY KEY CHECK ( code_cours SIMILAR TO 'BINV[0-9]{4}'),
    nom VARCHAR(40) NOT NULL,
    credits INTEGER NOT NULL CHECK ( credits > 0 ),
    bloc INTEGER NOT NULL CHECK ( bloc > 0 AND bloc < 4)
);

CREATE TABLE projet.etudiants (
    id_etudiant SERIAL PRIMARY KEY,
    nom VARCHAR(40) NOT NULL,
    prenom VARCHAR(40) NOT NULL,
    email_vinci VARCHAR(255) NOT NULL UNIQUE CHECK ( email_vinci SIMILAR TO '%@(student.)?vinci.be'),
    mdp VARCHAR(60) NOT NULL
);

CREATE TABLE projet.cours_etudiants (
    PRIMARY KEY (cours, etudiant),
    cours CHAR(8) NOT NULL REFERENCES projet.cours(code_cours),
    etudiant INTEGER NOT NULL REFERENCES projet.etudiants(id_etudiant)
);

CREATE TABLE projet.projets (
    id_projet VARCHAR(20) PRIMARY KEY,
    nom VARCHAR(40) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    CHECK ( date_debut < date_fin ),
    cours CHAR(8) NOT NULL REFERENCES projet.cours(code_cours)
);

CREATE TABLE projet.groupes (
    PRIMARY KEY (numero, projet),
    nombre_membres INTEGER NOT NULL DEFAULT 0,
    nombre_max_membres INTEGER NOT NULL,
    numero INTEGER NOT NULL,
    CHECK ( nombre_membres >= 0 AND nombre_membres <= groupes.nombre_max_membres AND nombre_max_membres > 0),
    est_valide BOOLEAN NOT NULL DEFAULT FALSE,
    projet VARCHAR(20) NOT NULL REFERENCES projet.projets(id_projet)
);

CREATE TABLE projet.groupes_etudiants (
    PRIMARY KEY (etudiant, projet),
    projet VARCHAR(20) NOT NULL,
    numero INTEGER NOT NULL,
    FOREIGN KEY (numero, projet) REFERENCES projet.groupes(numero, projet),
    etudiant INTEGER NOT NULL REFERENCES projet.etudiants(id_etudiant)
);

/*
    Application centrale
    1. Ajouter un cours
*/
CREATE OR REPLACE FUNCTION projet.ajouter_cours (_code_cours CHAR(8), _nom VARCHAR(40), _credits INTEGER, _bloc INTEGER) RETURNS VOID AS $$
DECLARE
BEGIN
    INSERT INTO projet.cours(code_cours, nom, credits, bloc)
    VALUES ($1, $2, $3, $4);
END;
$$ LANGUAGE plpgsql;

/*
    Application centrale
    2. Ajouter un étudiant
*/
CREATE OR REPLACE FUNCTION projet.ajouter_etudiant (_nom VARCHAR(40), _prenom VARCHAR(40), _email_vinci VARCHAR(255), mdp VARCHAR(60)) RETURNS VOID AS $$
DECLARE
BEGIN
    INSERT INTO projet.etudiants(nom, prenom, email_vinci, mdp)
    VALUES ($1, $2, $3, $4);
END;
$$ LANGUAGE plpgsql;

/*
    Application centrale
    3. Inscrire un étudiant à un cours
    RAISE si un cours a déjà un projet
*/
CREATE OR REPLACE FUNCTION projet.inscrire_etudiant_cours (_code_cours CHAR(8), _id_etudiant INTEGER) RETURNS VOID AS $$
DECLARE
BEGIN
    INSERT INTO projet.cours_etudiants (cours, etudiant)
    VALUES ($1, $2);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION projet.inscrire_etudiant_cours_trigger () RETURNS TRIGGER AS $$
DECLARE
BEGIN
    IF EXISTS (SELECT * FROM projet.projets pr WHERE pr.cours = NEW.cours)
    THEN
        RAISE 'Un étudiant ne peut être ajouté a un cours avec un projet existant';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER inscrire_etudiant_cours_trigger BEFORE INSERT ON projet.cours_etudiants
    FOR EACH ROW
    EXECUTE PROCEDURE projet.inscrire_etudiant_cours_trigger ();

/*
    Application centrale
    4. Créer un projet pour un cours
*/
CREATE OR REPLACE FUNCTION projet.creer_projet (_id_projet VARCHAR(20), _nom VARCHAR(40), _date_debut DATE, _date_fin DATE, code_cours CHAR(8)) RETURNS VOID AS $$
DECLARE
BEGIN
    INSERT INTO projet.projets (id_projet, nom, date_debut, date_fin, cours)
    VALUES ($1, $2, $3, $4, $5);
END;
$$ LANGUAGE plpgsql;

/*
    Application centrale
    5. Créer des groupes pour un projet
*/
CREATE OR REPLACE FUNCTION projet.creer_groupe_projet (_id_projet VARCHAR(20), _nombre_groupes INTEGER, _nombre_max_membres INTEGER) RETURNS VOID AS $$
DECLARE
    numero_groupe INTEGER;
BEGIN
    FOR counter IN 1 .. $2
    LOOP
        SELECT COALESCE(MAX(numero), 0) FROM projet.groupes WHERE projet = $1 INTO numero_groupe;

        INSERT INTO projet.groupes(numero, nombre_max_membres, projet)
        VALUES (numero_groupe + 1, $3, $1);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

/*
    1er SELECT récupère le code du cours
    2eme SELECT compte le nombre d'étudiants inscrits a ce cours
    3eme SELECT compte le nombre de places dans les groupes pour un projet

    RAISE si nombre de places dans les groupes du projet dépassent le nombre d'inscrits au cours
*/
CREATE OR REPLACE FUNCTION projet.creer_groupe_trigger () RETURNS TRIGGER AS $$
DECLARE
    nombre_etudiants INTEGER;
    nombre_places_groupes INTEGER;
    code_cours CHAR(8);
BEGIN
    SELECT pr.cours
    FROM projet.projets pr
    WHERE id_projet = NEW.projet
    INTO code_cours;

    SELECT COUNT(ce.etudiant)
    FROM projet.cours_etudiants ce
    WHERE ce.cours = code_cours
    INTO nombre_etudiants;

    SELECT SUM(gr.nombre_max_membres)
    FROM projet.groupes gr
    WHERE gr.projet = NEW.projet
    INTO nombre_places_groupes;

    IF (NEW.nombre_max_membres + nombre_places_groupes > nombre_etudiants)
    THEN
        RAISE 'Le nombre de places dans les groupes du projet dépassent le nombre d"inscrits au cours';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

/*
    Lance un trigger avant l'insertion dans la table groupes
*/
CREATE TRIGGER creer_groupe_trigger BEFORE INSERT ON projet.groupes
    FOR EACH ROW
    EXECUTE PROCEDURE projet.creer_groupe_trigger ();

/*
    Application centrale
    6. Visualiser les cours (code_cours, nom, et les identifiants des projets(sur une ligne séparé par des virgules))
    si le cours ne possède pas de projet il sera affiché 'pas encore de projet'
*/

CREATE OR REPLACE VIEW projet.visualiser_cours AS
    SELECT co.code_cours, co.nom, COALESCE(STRING_AGG(pr.id_projet, ', '), 'pas encore de projet') AS projets
    FROM projet.cours co LEFT OUTER JOIN projet.projets pr ON pr.cours = co.code_cours
    GROUP BY co.code_cours, co.nom;

/*
    Application centrale
    7. Visualiser tout les projets (identifiant, nom, code_cours, nombre de groupes, nombre de groupes complets, nombre de groupes validés)
*/
CREATE OR REPLACE VIEW projet.visualiser_projets AS
    SELECT pr.id_projet AS "id_projet", pr.nom AS "nom_projet", pr.cours AS "cours_projet", COUNT(gr.numero) AS "nombre_groupes", SUM(CASE WHEN gr.nombre_membres = gr.nombre_max_membres THEN 1 ELSE 0 END) AS "nombre_complets", SUM(CASE WHEN gr.est_valide = TRUE THEN 1 ELSE 0 END) AS "nombre_valide"
    FROM projet.projets pr
        LEFT OUTER JOIN projet.groupes gr ON pr.id_projet = gr.projet
    GROUP BY pr.id_projet, pr.nom, pr.cours;

/*
    Application centrale
    8. Visualser toutes les compositions de groupe d'un projet
    résultat sur 5 colonnes (numero_groupe, nom, prenom, si le groupe est complet, si le groupe a été validé)
    tout les numéros de groupes doivent apparaitre même si les groupes correspondants sont vides
    Si un groupe est vide, on affichera null pour (nom, prenom)
    triés par numero de groupe
*/
CREATE OR REPLACE VIEW projet.visualiser_groupes_projet AS
    SELECT gr.projet, gr.numero AS "Numéro", et.nom AS "Nom", et.prenom AS "Prénom", gr.nombre_membres = gr.nombre_max_membres AS "Complet ?", gr.est_valide AS "Validé ?"
    FROM projet.groupes gr
        LEFT OUTER JOIN projet.groupes_etudiants ge ON gr.numero = ge.numero AND gr.projet = ge.projet
        LEFT OUTER JOIN projet.etudiants et on et.id_etudiant = ge.etudiant
    ORDER BY ge.numero;

/*
    Application centrale
    9. Valider un groupe
*/
CREATE OR REPLACE FUNCTION projet.valider_groupe (_id_projet VARCHAR(20), _numero_groupe INTEGER) RETURNS VOID AS $$
DECLARE
BEGIN
    IF NOT EXISTS (SELECT * FROM projet.groupes gr WHERE gr.projet = $1 AND gr.numero = $2)
    THEN
        RAISE 'Ce groupe n"existe pas';
    END IF;

    UPDATE projet.groupes
    SET est_valide = TRUE
    WHERE numero = $2 AND projet = $1;
END;
$$ LANGUAGE plpgsql;

/*
    Application centrale
    10. Valider tout les groupes d'un projet
    Si un n'est pas complet -> aucun validé
*/
CREATE OR REPLACE FUNCTION projet.valider_groupes (_id_projet VARCHAR(20)) RETURNS VOID AS $$
DECLARE
    groupe RECORD;
BEGIN
    FOR groupe IN SELECT * FROM projet.groupes WHERE projet = $1
    LOOP
        IF(groupe.nombre_membres != groupe.nombre_max_membres)
        THEN
            RAISE 'Un des groupes n"est pas complet';
        END IF;
    END LOOP;

    FOR groupe IN SELECT * FROM projet.groupes WHERE projet = $1
    LOOP
        UPDATE projet.groupes
        SET est_valide = TRUE
        WHERE projet = $1;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

/*
    Vérifie que le groupe est complet avant de le valider
*/
CREATE OR REPLACE FUNCTION projet.valider_groupe_trigger () RETURNS TRIGGER AS $$
DECLARE
BEGIN
    IF (NEW.est_valide = TRUE AND NEW.nombre_membres != NEW.nombre_max_membres)
    THEN
        RAISE 'ce groupe n"est pas complet';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

/*
    Lance un trigger avant update dans groupes
*/
CREATE TRIGGER valider_groupe_trigger BEFORE UPDATE ON projet.groupes
FOR EACH ROW
EXECUTE PROCEDURE projet.valider_groupe_trigger ();

/*
    Application étudiant
    1. visualiser les cours auxquels il participe (code_cours, nom, les identifiants de ses projets)
*/
CREATE OR REPLACE VIEW projet.visualiser_cours_etudiant AS
    SELECT ce.etudiant, c.code_cours, c.nom, COALESCE(STRING_AGG(pr.id_projet, ', '), 'pas encore de projet') AS projets
    FROM projet.cours_etudiants ce
        LEFT OUTER JOIN projet.cours c ON c.code_cours = ce.cours
        LEFT OUTER JOIN projet.projets pr ON pr.cours = c.code_cours
    GROUP BY ce.etudiant, c.code_cours, c.nom;

/*
    Application étudiant
    2. inscrire l'étudiant au groupe choisi
 */
CREATE OR REPLACE FUNCTION projet.inscrire_etudiant_groupe (_projet varchar(20), _numero INTEGER, _id_etudiant INTEGER) RETURNS VOID AS $$
DECLARE
BEGIN
    INSERT INTO projet.groupes_etudiants (projet, numero, etudiant)
    VALUES ($1, $2, $3);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION projet.inscrire_etudiant_groupe_trigger () RETURNS TRIGGER AS $$
DECLARE
BEGIN
    IF EXISTS (SELECT * FROM projet.groupes gr WHERE gr.projet = NEW.projet AND gr.numero = NEW.numero AND gr.nombre_membres = nombre_max_membres)
    THEN
        RAISE 'Ce groupe est complet';
    END IF;

    IF NOT EXISTS (SELECT ce.* FROM projet.cours_etudiants ce, projet.projets pr WHERE pr.id_projet = NEW.projet AND ce.cours = pr.cours AND ce.etudiant = NEW.etudiant)
    THEN
        RAISE 'Vous n"etes pas inscris a ce cours';
    END IF;

    UPDATE projet.groupes SET nombre_membres = nombre_membres + 1 WHERE numero = NEW.numero AND projet = NEW.projet;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER inscrire_etudiant_groupe_trigger BEFORE INSERT ON projet.groupes_etudiants
FOR EACH ROW
EXECUTE PROCEDURE projet.inscrire_etudiant_groupe_trigger ();

/*
    Application étudiant
    3. retirer l'étudiant de son groupe pour le projet choisi
 */
CREATE OR REPLACE FUNCTION projet.retirer_etudiant_groupe (_projet varchar(20), _id_etudiant INTEGER) RETURNS VOID AS $$
DECLARE
BEGIN
    IF NOT EXISTS (SELECT * FROM projet.groupes_etudiants ge WHERE ge.projet = $1 AND ge.etudiant = $2)
    THEN
        RAISE 'Vous ne faites pas partie de ce projet';
    END IF;

    DELETE FROM projet.groupes_etudiants ge WHERE ge.etudiant = $2 AND ge.projet = $1;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION projet.retirer_etudiant_groupe_trigger () RETURNS TRIGGER AS $$
DECLARE
BEGIN
    IF EXISTS (SELECT * FROM projet.groupes gr WHERE gr.projet = OLD.projet AND gr.est_valide = TRUE)
    THEN
        RAISE 'Ce groupe a déja été validé';
    END IF;

    UPDATE projet.groupes
    SET nombre_membres = nombre_membres - 1
    WHERE projet = OLD.projet
        AND numero = (SELECT ge.numero
                    FROM projet.groupes_etudiants ge
                    WHERE ge.etudiant = OLD.etudiant
                        AND ge.projet = OLD.projet);

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER retirer_etudiant_groupe_trigger BEFORE DELETE ON projet.groupes_etudiants
FOR EACH ROW
EXECUTE PROCEDURE projet.retirer_etudiant_groupe_trigger ();

/*
    Application étudiant
    4. Visualiser tout les projets des cours d'un étudiant (id, nom, code_cours, numero de groupe)
    si pas de groupe -> numero de groupe = NULL
*/
CREATE OR REPLACE VIEW projet.visualiser_projets_etudiant AS
    SELECT ce.etudiant, pr.id_projet, pr.nom, pr.cours, ge.numero
    FROM projet.cours_etudiants ce
        INNER JOIN projet.projets pr ON ce.cours = pr.cours
        LEFT OUTER JOIN projet.groupes_etudiants ge on ce.etudiant = ge.etudiant AND ge.projet = pr.id_projet;

/*
    Application étudiant
    5. Visualiser tout les projets pour lesquels l'étudiant n'a pas de groupe (id, nom, code_cours, date début, date fin)
*/
CREATE OR REPLACE VIEW projet.visualiser_projets_sans_groupe_etudiant AS
    SELECT ce.etudiant, pr.id_projet, pr.nom, pr.cours, pr.date_debut, pr.date_fin
    FROM projet.cours_etudiants ce, projet.projets pr
    WHERE ce.cours = pr.cours
    AND pr.id_projet NOT IN (SELECT ge.projet
                             FROM projet.groupes_etudiants ge
                             WHERE ge.projet = pr.id_projet
                               AND ge.etudiant = ce.etudiant);

/*
    Application étudiant
    6. Visualiser toutes les compositions de groupes incomplets d’un projet
*/
CREATE OR REPLACE VIEW projet.visualiser_composition_groupes_incomplets AS
    SELECT gr.projet, gr.numero, et.nom, et.prenom, gr.nombre_max_membres - gr.nombre_membres AS "Nombre places"
    FROM projet.groupes gr
        LEFT OUTER JOIN projet.groupes_etudiants ge on gr.numero = ge.numero and gr.projet = ge.projet
        LEFT OUTER JOIN projet.etudiants et on et.id_etudiant = ge.etudiant
    WHERE gr.nombre_max_membres - gr.nombre_membres > 0;

GRANT CONNECT ON DATABASE dbfrancoisvandeputte TO damienlapinski;
GRANT USAGE ON SCHEMA projet TO damienlapinski;

GRANT SELECT ON ALL TABLES IN SCHEMA projet TO damienlapinski;

GRANT SELECT ON projet.visualiser_cours_etudiant TO damienlapinski;
GRANT SELECT ON projet.visualiser_projets_etudiant TO damienlapinski;
GRANT SELECT ON projet.visualiser_projets_sans_groupe_etudiant TO damienlapinski;
GRANT SELECT ON projet.visualiser_composition_groupes_incomplets TO damienlapinski;

GRANT INSERT ON TABLE projet.groupes_etudiants TO damienlapinski;
GRANT DELETE ON TABLE projet.groupes_etudiants TO damienlapinski;

GRANT UPDATE ON TABLE projet.groupes TO damienlapinski;

SELECT projet.ajouter_cours ('BINV2040', 'BD2', 6, 2);
SELECT projet.ajouter_cours ('BINV1020', 'APOO', 6, 1);

SELECT projet.ajouter_etudiant ('Damas', 'Christophe', 'cd@student.vinci.be', '$2a$10$HaM3EwbG8Y70m0ulzSIsXOt6.r3P2xoMHyZ159G63xlVTukAmOhGy');
SELECT projet.ajouter_etudiant ('Ferneeuw', 'Stéphanie', 'sf@student.vinci.be', '$2a$10$HaM3EwbG8Y70m0ulzSIsXOt6.r3P2xoMHyZ159G63xlVTukAmOhGy');

SELECT projet.inscrire_etudiant_cours ('BINV2040', 1);
SELECT projet.inscrire_etudiant_cours ('BINV2040', 2);

SELECT projet.creer_projet ('projSQL', 'projet SQL', '10/09/2023', '15/12/2023', 'BINV2040');
SELECT projet.creer_projet ('dsd', 'DSD', '30/09/2023', '01/12/2023', 'BINV2040');

SELECT projet.creer_groupe_projet ('projSQL', 1, 2);
