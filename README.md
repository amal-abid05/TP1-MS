# TP1 : Inversion de Contrôle et Injection de Dépendances

**Matière :** Architecture MicroServices, MPSRCC1 · **Outils :** JDK 17, IntelliJ IDEA, Maven, Spring

---

## 0. Préparation de l'environnement

**Étape 0.1 : vérifier Java.** Dans un terminal :

```
java -version     # doit afficher 17.x
javac -version    # doit afficher javac 17.x
```

**Étape 0.2 : créer le projet.** IntelliJ → *New Project* → **Java** → ** (Maven) ** → JDK **17** → Name : `TP1-MS` → *Create*.

**Étape 0.3 : créer les packages.** Clic droit sur `src/main/java` → *New → Package* : `dao`, `metier`, `presentation`.

**Étape 0.4 : créer le dossier de ressources** (s'il n'existe pas). Clic droit sur `src/main` → *New → Directory* → `resources`. Si le dossier n'est pas reconnu, clic droit → *Mark Directory as → Resources Root*.

**Étape 0.5 : `pom.xml` complet** (utilisé dès le départ, ce qui évite de le modifier plus tard) :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>TP1</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring.version>6.2.8</spring.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <version>${spring.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-beans</artifactId>
            <version>${spring.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
            <version>${spring.version}</version>
        </dependency>
    </dependencies>
</project>
```

Puis cliquer sur **Load Maven Changes** (icône Maven en haut à droite de l'éditeur). Toute version Spring 6.x convient avec Java 17 ; Spring 6 exige au minimum JDK 17.

---

## Activité 1-1 : Injection de dépendances par instanciation statique

### Étape 1 : l'interface `IDao` (package `dao`)

```java
package dao;

public interface IDao {
    double getValue();
}
```

### Étape 2 : la classe `DaoIMP` (package `dao`)

```java
package dao;

public class DaoIMP implements IDao {
    @Override
    public double getValue() {
        return 3;   // simulation d'un accès aux données
    }
}
```

### Étape 3 : l'interface `IMetier` (package `metier`)

```java
package metier;

public interface IMetier {
    double calcul();
}
```

### Étape 4 : la classe `MetierIMP` (package `metier`)

```java
package metier;

import dao.IDao;

public class MetierIMP implements IMetier {

    private IDao dao;   // dépendance vers l'INTERFACE : couplage faible

    @Override
    public double calcul() {
        double val = dao.getValue();
        return val * 7;
    }

    // Setter utilisé pour l'injection
    public void setDao(IDao dao) {
        this.dao = dao;
    }
}
```

> ⚠️ Ne jamais écrire `private IDao dao = new DaoIMP();` : cela recréerait un couplage fort.

### Étape 5 : `PresentationStatique` (package `presentation`)

```java
package presentation;

import dao.DaoIMP;
import dao.IDao;
import metier.MetierIMP;

public class PresentationStatique {
    public static void main(String[] args) {
        IDao dao = new DaoIMP(); // instanciation statique du DAO
        IMetier metier = new MetierIMP(); // instanciation statique du métier

        ((MetierIMP) metier).setDao(dao); // INJECTION de dépendance

        System.out.println(metier.calcul());
    }
}
```

### Étape 6 : exécution

Clic sur le triangle vert ▶ à côté de `main` → *Run 'PresentationStatique.main()'*. **Résultat attendu : `21.0`** (3 × 7).

### Étape 7 : explication (question 9 du TP)

- `MetierIMP` ne connaît que l'interface `IDao`, jamais `DaoIMP`. Il ne dépend donc que d'un **contrat** (`getValue()`), pas d'une implémentation.
- On peut créer `DaoMySQL`, `DaoWebService`, etc., qui implémentent `IDao`, **sans modifier une seule ligne de `MetierIMP`** : l'application est **fermée à la modification et ouverte à l'extension**.
- C'est la classe `PresentationStatique` qui choisit l'implémentation et la fournit à `MetierIMP` via `setDao()` : c'est l'**injection de dépendances**.
- Sans cette injection, `dao` vaudrait `null` et `calcul()` lèverait une `NullPointerException`.
- Limite : le choix de `DaoIMP` est encore écrit en dur dans `PresentationStatique` (`new DaoIMP()`), donc changer d'implémentation oblige à recompiler. D'où l'activité 1-2.

---

## Activité 1-2 : Injection par instanciation dynamique

### Étape 1 : créer `config.txt`

Clic droit sur `src/main/resources` → *New → File* → `config.txt` :

```
dao.DaoIMP
metier.MetierIMP
```

Les noms doivent correspondre **exactement** (casse et package) à vos classes.

### Étape 2 : `PresentationDynamique` (package `presentation`)

```java
package presentation;

import dao.IDao;
import metier.IMetier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

public class PresentationDynamique {
    public static void main(String[] args) throws Exception {

        // 1. Lire le fichier config.txt depuis le classpath
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        PresentationDynamique.class
                                .getClassLoader()
                                .getResourceAsStream("config.txt")));

        String daoClassName = br.readLine();      // dao.DaoIMP
        String metierClassName = br.readLine();   // metier.MetierIMP
        br.close();

        // 2. Charger dynamiquement les classes et créer les instances
        Class<?> cDao = Class.forName(daoClassName);
        IDao dao = (IDao) cDao.getDeclaredConstructor().newInstance();

        Class<?> cMetier = Class.forName(metierClassName);
        IMetier metier = (IMetier) cMetier.getDeclaredConstructor().newInstance();

        // 3. Injection : appel dynamique de setDao(IDao) par réflexion
        Method setDao = cMetier.getMethod("setDao", IDao.class);
        setDao.invoke(metier, dao);

        // 4. Utilisation
        System.out.println(metier.calcul());
    }
}
```

L'injection par réflexion (`getMethod("setDao", IDao.class)`) évite le cast vers `MetierIMP` : le code ne dépend alors plus du tout des classes concrètes.

### Étape 3 : exécution

**Résultat attendu : `21.0`.**

### Étape 4 : explication (question 7 du TP)

- Le code Java ne contient plus aucun `new DaoIMP()` ni `new MetierIMP()`. Les **noms de classes sont lus dans `config.txt`**, chargés avec `Class.forName()`, puis instanciés par réflexion.
- Pour changer d'implémentation (par ex. `dao.DaoIMP2`), il suffit de modifier `config.txt` : **aucune modification ni recompilation** du code de présentation.
- L'application est donc réellement ouverte à l'extension. C'est le principe de ce que fera Spring de manière industrialisée.

---

## Activité 1-3 : Spring avec fichier de configuration XML

### Étape 1 : vérifier les dépendances

Spring core, beans et context sont déjà dans le `pom.xml` (étape 0.5). Vérifier qu'elles apparaissent dans *External Libraries*.

### Étape 2 : créer `config.xml`

Dans `src/main/resources` (le sujet écrit « package ressource » : c'est bien le dossier `resources`) :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       https://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- Bean DAO -->
    <bean id="dao" class="dao.DaoIMP"/>

    <!-- Bean métier, avec injection du DAO via le setter setDao() -->
    <bean id="metier" class="metier.MetierIMP">
        <property name="dao" ref="dao"/>
    </bean>
</beans>
```

### Étape 3 : `PresentationAvecSpring` (package `presentation`)

```java
package presentation;

import metier.IMetier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class PresentationAvecSpring {
    public static void main(String[] args) {
        ApplicationContext context =
                new ClassPathXmlApplicationContext("config.xml");

        IMetier metier = context.getBean("metier", IMetier.class);
        System.out.println(metier.calcul());
    }
}
```

### Étape 4 : exécution

**Résultat attendu : `21.0`.**

### Étape 5 : explication (question 6 du TP)

1. `ClassPathXmlApplicationContext("config.xml")` crée le **conteneur IoC** : il lit le XML dans le classpath.
2. Spring instancie chaque `<bean>` (via réflexion) : `DaoIMP` puis `MetierIMP`.
3. Pour `<property name="dao" ref="dao"/>`, Spring appelle `setDao(...)` sur `MetierIMP` en lui passant le bean `dao` : c'est l'**injection de dépendances par setter**.
4. `getBean("metier", IMetier.class)` retourne l'objet déjà assemblé.
5. Le contrôle de la création et de l'assemblage est passé du programmeur au conteneur : c'est l'**inversion de contrôle**.

> Pour que `<property name="dao">` fonctionne, `MetierIMP` doit avoir un setter `setDao` et un constructeur par défaut.

---

## Activité 1-4 : Spring avec annotations

### Étape 1 : annoter `DaoIMP`

```java
package dao;

import org.springframework.stereotype.Repository;

@Repository
public class DaoIMP implements IDao {
    @Override
    public double getValue() {
        return 3;
    }
}
```

### Étape 2 : annoter `MetierIMP`

```java
package metier;

import dao.IDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetierIMP implements IMetier {

    @Autowired
    private IDao dao;

    @Override
    public double calcul() {
        return dao.getValue() * 7;
    }

    public void setDao(IDao dao) {   // conservé pour que les activités 1-1 à 1-3 fonctionnent encore
        this.dao = dao;
    }
}
```

### Étape 3 : `PresentationAvecAnnotation` (package `presentation`)

```java
package presentation;

import metier.IMetier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class PresentationAvecAnnotation {
    public static void main(String[] args) {
        // Scan des packages contenant les classes annotées
        ApplicationContext context =
                new AnnotationConfigApplicationContext("dao", "metier");

        IMetier metier = context.getBean(IMetier.class);
        System.out.println(metier.calcul());
    }
}
```

### Étape 4 : exécution

**Résultat attendu : `21.0`.** Ici `config.xml` n'est plus utilisé.

### Étape 5 : explication (question 5 du TP)

- `AnnotationConfigApplicationContext("dao", "metier")` **scanne** ces packages à la recherche de classes annotées par un *stéréotype* (`@Repository`, `@Service`, `@Component`, `@Controller`).
- Chaque classe trouvée devient un **bean** géré par Spring : `@Repository` marque la couche d'accès aux données, `@Service` la couche métier.
- `@Autowired` sur `private IDao dao` demande à Spring de chercher un bean compatible avec le type `IDao` (ici `DaoIMP`) et de l'injecter automatiquement.
- Plus de XML ni de `setDao()` explicite : la configuration est portée par le code.
- Si plusieurs beans implémentent `IDao`, Spring signale une ambiguïté ; on la résout avec `@Qualifier` ou `@Primary`.

---

## Structure finale du projet

```
TP1
├── pom.xml
└── src/main
    ├── java
    │   ├── dao            IDao.java, DaoIMP.java
    │   ├── metier         IMetier.java, MetierIMP.java
    │   └── presentation   PresentationStatique.java
    │                      PresentationDynamique.java
    │                      PresentationAvecSpring.java
    │                      PresentationAvecAnnotation.java
    └── resources          config.txt, config.xml
```

## Tableau de synthèse

| Activité | Qui crée et assemble les objets ? | Mécanisme |
| --- | --- | --- |
| 1-1 Statique | Le programmeur | `new` + `setDao()` |
| 1-2 Dynamique | Le programme, via la configuration | `config.txt` + `Class.forName()` |
| 1-3 Spring XML | Le conteneur Spring | `config.xml` (`<bean>`, `<property>`) |
| 1-4 Annotations | Le conteneur Spring | `@Repository`, `@Service`, `@Autowired` |

**À retenir :** couplage faible (dépendre d'interfaces) → injection de dépendances (recevoir sa dépendance de l'extérieur) → inversion de contrôle (un conteneur crée et assemble les objets).

