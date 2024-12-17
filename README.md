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
prometheus za centaliziran pracenej umjesto logirana [x]


Eh107Configuration je cesto nepotreban kod moderne hibernate jcache-a (provjeiti) []

dan 3 [x]
napredni peristance layer [x]
criteria api i dinamicki upiti [x]
PostgreSQL tsvector [x]
 razimislit gdje je potrbena JPA Projekcije []
jpa napredno [x]
dodao redis kesiranje s coffeine [x]

razmisliti jesam li malo pretjerao s peformancima i naprednim persisten znacajkama (vjv jesam  :) [y]

dan 4 [y]
verzioniranje api-a [x]
intrecpeoti [x]
dodati ratelimiter [x]
dodati spring hateos te [x]


dan 5 [x]
TODO ispraviti i poboljsati visak ili dupli kod [x]
centralizirati [x]
pojednostaviti [x]

dan 6 [x]
jos jednomo opeimizirati prva 4 dana maknuti unused inport itd [x]
optimizacija koda jos jednom [] (primjetio sam da imam duplikate negdje i da se neke metode mogu maknuti i neke klase rastavi)
dodati poboljsanja za otimizaciju i metrike [x]
provjeriti pratim li moderne tehnike  svugjde [x]
optimizaciaj repository i servisa [x]

dan 7[x]
poboljsati kratko ratelimiter []
provjeriti i testirai kontrollere [-]
nastaivit optimizaciju i healt check kesiranja “[x]
poboljsati rad s eventima [x]


dan 8 [x]
unaprijediti query i njegovu metriku te pracenje analize [x]
unaprijediti sustav za napredne znacajke za pretrazivanje i analitiku []
Skuziti zasto micrometer vraca krivo vrijeme query npr 30sec mi vraca a izvede se u 0.73 ms [x]

dan 9 [x]
DDD promjenio domenske modele i logkiku [x]
promjenio dtos pazio na ciruralne referenec i uuuid vs string za konverziju [x]
razimilio o boljoj validicaiji i promjeni mappera i servisa [x]


dan 10 i 11 [x]
proci kroz course servis i testirti svu validaciju [x]
testirati i dali rade ispravno [x]




Postaviti eureka servis []
Razmisilit o GrallVM za native images []
Implementirati kafka za event streamanje [x]
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

- Misli i objasnjejna

Arhitektura se temelji na modularnom dizajnu koji ce implementirai DDD "domain driven arch" te ce se sastojati od nekoliok kljucnih slojeva

 Domenki sloj -> mozemo reci da prestavlja srce sustava 

 * Course entitet djelu kao agregat root koja implementira domensku logiku

 Event sustav implementira event driven arhc koja omogcava labavo povezanosti izmedu komponenti 
 
 * npr svaki zasebni događaj poput kreacije ili objave je zasbeni event objekt koji nasljeduje baseEvent klase a moja CourseEventHandler ih
    osluskije i reagira na njih

 Inrfastrukturni sloj ukratko sloj koji veliku pažnju daje na performance i skalabilnost s npr klasa koje sam vec implementirao kao CentralizedCacheConfiguration 

* kombiniraj 2 layer kes sloj lokalni caffeine ili ti in memory i redis distubituvni te strategije kesiranja dekoratora MonitoredCache klase koj dodaje metrike
  bez narusavanja performansi (nadam se)

servisni sloj ce implemenitrati klasika poslovnu logiku uz optimalnu upravljanje resurisima baze podatka i robustan sustav rukavanje greskama

 * npr imam partitionMigrationService tu koji ce upravljati efikasno velikom kolicniom podatka kroz particioniranje tabliva

 Api sloj implementira Rest principe s verzioniranje i dokimentacijom uz korsitenje DTO objekta za transfer podakta 

 * npr tu imamo ratelimiter koji stiti api od prepoterecenja

 Sustav za napredne tehnikce kao npr full text search oslanavajuci se na postgresql featurse 

 * ukratko pracenje i optimizacija performanski upita uz indeksiranje kes itd..

 Konfiguracija Korsiti app yml za stukturirano hirearhiju te docker compose koji mi omogucuje konzistetno stanje za razvoj i prod

 * tu se npr second-levle cache conncetion-pool za optimizacij ubaze i monotoring i sam dokcker compose s kafka redis i postgres servisima

 Imam robusno rukovanje greskama na vise razina npr domenska iznimki do globalnog error hanldera 

 * tu spada i validaciaj koje se isto kao i iznimke provodi na vise razina 

 * Pokusao sam pratiti koliko god je moguce Solid prinicepe i dizajn pbrasci kao npr:

 * Srv ili ti single responsibily princip - entiteti fokusirani samo na domenka pravila servisi na poslvona itd.
 * OCP ili ti open/closed prinicple - koristenje abstrakciaj i sucelja koje omogucuje dodavanje novih izmjena bez postoječega koda
 * LSP liskov - hirearhija ili ti pravila nasljedivanja base model entiet pruza zajednicko posnasnjae koje ostale klase porsiruju
 * ISP interface segeration - repository interface su segmentirani prema funkionalnosti 
 * DI dep injection - kroz construktore 
 * Reatcie programing i asny uz virtual threads i kafku 



dodao redis i coffeince za 1 i 2 lazer kesiranja dodao optimizacije dinamciki criterije s criterije builder jpa proekcije 
 ze indexe i metrcie te pracenje svega razmisilit o tome da maknem nesto vjv ima viska kad budem dosao do toga 


 Višeslojni kes, sustav kombinira caffeine za lokalno predmemoriranje i redis za distibutivno 

Event Driven sustav korsiti kombinaciju Spring Event-a i Kafk-u za distribuirane događaje
 -> Base model je indirektno povezan s ovim sustavom kroz entitete koji ga nasljeđuju
    -> Posebno Course model koji generira događaje  
      -> sustav za particioniranje koji upravlja podacima kroz vrijeme gjde basemodel pruza vremenske oznake createtAt i updatedAt


 Sto vise dodajem kompleksno to me vise zarinjavaj urace conditionoal 

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


