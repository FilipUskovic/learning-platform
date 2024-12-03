# learning-platform

# Getting Started


TODO:
Kreirati repo [x]
Kreirati docker compose volume i image container [x]
Povezati i postaviti postgres [x]
Razmisliti o virtual threads [x]
Razmisliti o reactive programing [x]
Dan 1 zavrsiti [x]

Postaviti Course enteti i dto [x]
course repository i excpetion [x]
course serce klass [x]
dodati kontroler i validacije za course [x]
Audit i event za course [x]
Testirai dan 1 crud za sad ne dodavati jos validacija i poboljsaje nego sam test []
Kasnije s monolotich arch uvesti mircoservis arch [x]
Dan 2 [x]
azuriatu flyway [x]
prositit domenski sloj [x]
dodati batch inserte [x]
dodati i razmislit trebaju mi pracenej perofrmanci [x]
isto tako treba li mi second level kseiranje [x]
dodao particioniranje [x]
provjeriti dan 2 ponovno []
prometheus za centaliziran pracenej umjesto logirana []


Eh107Configuration je cesto nepotreban kod moderne hibernate jcache-a (provjeiti) []

dan 3 []
napredni peristance layer [x]
criteria api i dinamicki upiti [x]
PostgreSQL tsvector [x]
JPA Projekcije []
jpa napredno []

razmisliti jesam li malo pretjerao s peformancima i naprednim persisten znacajkama (vjv jesam  :) []

Postaviti eureka servis []
Razmisilit o GrallVM za native images []
Implementirati kafka za event streamanje []
Implementirati redis za kesiranje []
Dodati Produkciski sloj i env file []
Kasnije dodati virtuhal threads i reactive approcah []
poboljsati strukutru []


Virtual threads -> Visoka evikasonst (lakse od tradicionalnih threadova), velik broj istovremenih threads
-> jednostavnije => nema potrebe za kompleksnim asnsinkronim kodom (jednostavni razvoj i odrzavanje app)
-> Bolja skalabilnost (omogucuje horizontalno skaliranje)

Reactive ili nebolokiracuje rukovanje podacima i dogadajima
-> asinkonost => operacije se izvrsavaju bez blokiranja thread-a, veca konkuretnost
-> neblokirajuci i/o za app koje puno citaju/pisu u bazu
-> reaktivni streams - standaniziran api


batch iserti mi omgucujavaju ubacivanje velikih kolicnia bez da moramo svaki put se spajati na bazu slati zahtije i podatke
eikasnije je u tome

    -> Koristimo batch processing za efikasno kreiranje većeg broja modula
    -> Izbjegavamo učitavanje cijelog course objekta u memoriju kada radimo s modulima
    -> Koristimo native queries za optimalne performanse kod čitanja
    -> Implementiramo pravilno upravljanje persistence kontekstom da izbjegnemo memory leaks


korsiti cu oba pristupa iako povecava znatno slozenost  jer :
-> zelim nauciti bolje reactive, a i zelim okusati virtual threads
-> koristi virtual threads za za pisanje jednostavnog sekvencionalnog koda gdje kod je moguce
-> korsiti reactive pristup za specificne zahtijevne operacije  i rad s kafkom
->


PostgreSQL tsvector :
 -> implementiraju full text pretagu
 -> brzi nego like pretrazvianja 


echo "# learning-platform" >> README.md
git init
git add README.md
git commit -m "first commit"
git branch -M main
git remote add origin https://github.com/FilipUskovic/learning-platform.git
git push -u origin main


* Promjena s maven na grade built tool (da naucim novo) a i posto cu kasnije imati grallVM pa
  se cini bolji izbor jer :
  -> korsiti DSL (domain specific language ) za konfiguraciju (fleksibilni i expresivni)
  -> faster build i paralelizacija
  -> dep su dohvacani s maven repo-a
  -> bolje performance vs maven i ima incremental build
  -> vec spomenuto fleksibilnija strukutra i dinamicki dependecije

*



### Reference Documentation

For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.4.0/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.4.0/gradle-plugin/packaging-oci-image.html)
* [GraalVM Native Image Support](https://docs.spring.io/spring-boot/3.4.0/reference/packaging/native-image/introducing-graalvm-native-images.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/3.4.0/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/3.4.0/reference/using/devtools.html)
* [Docker Compose Support](https://docs.spring.io/spring-boot/3.4.0/reference/features/dev-services.html#features.dev-services.docker-compose)
* [Spring Web](https://docs.spring.io/spring-boot/3.4.0/reference/web/servlet.html)
* [Spring Reactive Web](https://docs.spring.io/spring-boot/3.4.0/reference/web/reactive.html)

### Guides

The following guides illustrate how to use some features concretely:

* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Building a Reactive RESTful Web Service](https://spring.io/guides/gs/reactive-rest-service/)

### Additional Links

These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
* [Configure AOT settings in Build Plugin](https://docs.spring.io/spring-boot/3.4.0/how-to/aot.html)

### Docker Compose support

This project contains a Docker Compose file named `compose.yaml`.
In this file, the following services have been defined:

* postgres: [`postgres:latest`](https://hub.docker.com/_/postgres)

Please review the tags of the used images and set them to the same as you're running in production.

## GraalVM Native Support

This project has been configured to let you generate either a lightweight container or a native executable.
It is also possible to run your tests in a native image.

### Lightweight Container with Cloud Native Buildpacks

If you're already familiar with Spring Boot container images support, this is the easiest way to get started.
Docker should be installed and configured on your machine prior to creating the image.

To create the image, run the following goal:

```
$ ./gradlew bootBuildImage
```

Then, you can run the app like any other container:

```
$ docker run --rm -p 8080:8080 learning-platform:0.0.1-SNAPSHOT
```

### Executable with Native Build Tools

Use this option if you want to explore more options such as running your tests in a native image.
The GraalVM `native-image` compiler should be installed and configured on your machine.

NOTE: GraalVM 22.3+ is required.

To create the executable, run the following goal:

```
$ ./gradlew nativeCompile
```

Then, you can run the app as follows:

```
$ build/native/nativeCompile/learning-platform
```

You can also run your existing tests suite in a native image.
This is an efficient way to validate the compatibility of your application.

To run your existing tests in a native image, run the following goal:

```
$ ./gradlew nativeTest
```

### Gradle Toolchain support

There are some limitations regarding Native Build Tools and Gradle toolchains.
Native Build Tools disable toolchain support by default.
Effectively, native image compilation is done with the JDK used to execute Gradle.
You can read more
about [toolchain support in the Native Build Tools here](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html#configuration-toolchains).


