package com.micro.learningplatform.models;

import com.micro.learningplatform.event.course.CourseCreatedEvent;
import com.micro.learningplatform.event.course.CourseModuleAddedEvent;
import com.micro.learningplatform.event.course.CourseStatusChangedEvent;
import com.micro.learningplatform.shared.exceptions.CourseStateException;
import com.micro.learningplatform.shared.exceptions.CourseValidationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cache;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "courses")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString
//Secong level cache za smanjejne opterecenja baze
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE) // -> read i write strategoija osigurava da su podaci dosljedni onima u bazi
public class Course extends BaseModel{

    /* 1. Poboljšanja enkapsulacija
          -> getteri i setteri postavljeni na protected razinu
          -> imao factory pattern za kreiranje instanic
       2. Bolja interakfijca s eventima
          -> događaji se registirraju pri znacajnim promjenama stanja
          -> korsitimo nasljedeni mehanizam iz basemodule klase
       3. Optimiziranje predmemorije
          -> implementaije getCacheKey za jednoznačnu idnetifikaicju u kesiranju
          -> BatchSize za optimizaciju ucitavanje povjesnih statistika
       4. Podrška particioniranje i poboljšanja analiza upita
          -> Implementiran getPartitionKey za integraciju s vašim sustavom particioniranja
          -> Proširena metadata za analizu upita s dodatnim informacijama specifičnim za tečajeve
        5. Koristio sam embbede jer su lakse za bazu, tj smanjuj broj upita prema bazi hvataju u jednom upitu sve
     */

    private static final int MINIMUM_MODULES_FOR_PUBLICATION = 1;


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID Id;

    @Size(min = 1, max = 200)
    @Column(nullable = false, unique = true)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus courseStatus;

    /**
     * mappedBy = "course" polje course u CourseModule je vlasnik veze
     * ascade znaci da se sve promjene (del, inser, updae) automatksi propagiraju na povezane module
     * orphanRemoval = true: kad ase modul ukloni iz kolekcije automatski se briše iz base
     * aktiviram keš za kolekciju, smanjujem broj upita prema bazi
     *
     */

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @ToString.Exclude
    private Set<CourseModule> modules = new HashSet<>();

    // ugrađena klasa CourseStatistics, nema vlastitu talbicu


    // brzi pristup osnovnim statistikama
    @Embedded
    private CourseStatisticsSnapshot statisticsSnapshot;

    // detaljnije statiske su sada zaspebni entiet i tu korsiti separation of concer sada
    @OneToOne(mappedBy = "course", cascade = CascadeType.ALL)
    private CourseStatistics courseStatistics;

    /**
     * mappedBy = "course" polje course u CourseStatisticHistory je vlasnik veze
     * ascade znaci da se sve promjene (del, inser, updae) automatksi propagiraju na povezane module
     * optimiziram dohvat samo 20 zapisa odjenom
     *
     */

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @OrderBy("snapshotTimestamp DESC")
    @BatchSize(size = 20)
    @ToString.Exclude
    private List<CourseStatisticHistory> statisticHistory = new ArrayList<>();


    protected Course(){
        this.courseStatus = CourseStatus.DRAFT;
        setCategory(EntityCategory.COURSE);
        this.statisticsSnapshot = new CourseStatisticsSnapshot();
    }

    // Factory metoda koja osigurava ispravnu inicijalizaciju
    public static Course create(String title,  String description) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.courseStatistics = new CourseStatistics(course);
        course.registerEvent(new CourseCreatedEvent(course.getId()));
        return course;
    }

    // Domenski  metode/logika

    // valditira i dodajem module, te azurira statisitku, kreira snap i registira događaj
    public void addModule(CourseModule module) {
        validateModuleAddition(module);
        module.setCourse(this);
        modules.add(module);
        statisticsSnapshot.incrementModuleCount();
        statisticsSnapshot.addDuration(module.getDuration());
        courseStatistics.recalculate(modules);
        createSnapshot();
        registerEvent(new CourseModuleAddedEvent(this.getId(), module.getId()));
    }


    // za uklananje modula TODO: vidjeti dali samo administrator moze uklanjati module
    public void removeModule(CourseModule module) {
        validateModuleRemoval(module);
        modules.remove(module);
        module.setCourse(null);
        statisticsSnapshot.decrementModuleCount();
        statisticsSnapshot.subtractDuration(module.getDuration());
        courseStatistics.recalculate(modules);
        createSnapshot(); // Stvara snimku nakon izmjene

        //registerEvent(new CourseModuleRemovedEvent(this.getId(), module.getId())); // Registrira događaj
    }

   // Provjerava uvjete za objavu, mijenja status i registrira događaj promjene statusa.
    public void publish() {
        validateForPublication();
        CourseStatus previousStatus = this.courseStatus;
        this.courseStatus = CourseStatus.PUBLISHED;
        registerEvent(new CourseStatusChangedEvent(this.getId(), previousStatus, CourseStatus.PUBLISHED));
    }


    @Override
    protected boolean isEditableState() {
        return CourseStatus.DRAFT.equals(this.courseStatus);
    }

    // Privatne metode za validaciju
    private void validateModuleAddition(CourseModule module) {
        if (!isEditableState()) {
            throw new CourseStateException("Moduli can be added only for coourse with DRAFT status");
        }
        Objects.requireNonNull(module, "Modul cannot be null");
    }

    private void validateModuleRemoval(CourseModule module) {
        if (!isEditableState()) {
            throw new CourseStateException("Moduli can be removed only in courses in DRAFT status");
        }
        Objects.requireNonNull(module, "Modul cannot be null");
        if (!modules.contains(module)) {
            throw new IllegalArgumentException("Modul is not part of this courses");
        }
    }

    private void validateForPublication() {
        if (!isEditableState()) {
            throw new CourseStateException("Courses can be published only from DRAFT status");
        }
        if (modules.size() < MINIMUM_MODULES_FOR_PUBLICATION) {
            throw new CourseValidationException(Collections.singletonList("Courses must have  atleast " +
                    MINIMUM_MODULES_FOR_PUBLICATION + " modul for publish"));
        }
    }

    // Metode za podršku predmemoriranju
    @Override
    public String getCacheKey() {
        return "course:" + getId();
    }

    // Metode za podršku particioniranju
    @Override
    public LocalDateTime getPartitionKey() {
        return getCreatedAt();
    }

    // Metode za podršku analizi upita
    @Override
    public Map<String, Object> getQueryMetadata() {
        Map<String, Object> metadata = super.getQueryMetadata();
        metadata.put("courseStatus", this.courseStatus);
        metadata.put("moduleCount", this.modules.size());
        return metadata;
    }


    private void createSnapshot() {
        CourseStatisticHistory snapshot = CourseStatisticHistory.createSnapshot(courseStatistics);
        statisticHistory.add(snapshot); // Koristimo add jer je lista sortirana s @OrderBy
    }

    public List<CourseStatisticHistory> getStatisticHistory(LocalDateTime startDate, LocalDateTime endDate) {
        return statisticHistory.stream()
                .filter(history -> history.getSnapshotTimestamp().isAfter(startDate) &&
                        history.getSnapshotTimestamp().isBefore(endDate))
                .collect(Collectors.toList());
    }


    /*
      * Klasa Course implementira napredne DDD principe poput validacije, keširanja, događaja, i statistike.
      * Omogućuje kontrolirano upravljanje tečajevima kroz modularnu logiku.
      * omogucava proširivost, održavanje i skalabilnost za kompleksne sustave.
     */


    /*
    private static final int MINIMUM_MODULES_FOR_PUBLICATION = 1;
    private static final Logger log = LogManager.getLogger(Course.class);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    //TODO: razmisliti o dodavanju sequence generatora
    private UUID Id;

    @Size(min = 1, max = 200)
    @Column(nullable = false, unique = true)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus courseStatus;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("sequenceNumber")
    // Secong level cache za smanjejne opterecenja baze
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @ToString.Exclude
    private List<CourseModule> modules = new ArrayList<>();

    @Embedded
    private CourseStatistics statistics;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @OrderBy("snapshotTimestamp DESC")
    @ToString.Exclude
    private List <CourseStatisticHistory> statisticHistory = new ArrayList<>();

    // zelim da statisitka uvijek postoji
    protected Course() {
        this.statistics = new CourseStatistics();
    }

    //TODO: ostaviti samo osnovnu domensku logiku, ostalo premjesiti prema ddd prinicipma

    // Private konstruktor za factory metodu
    private Course(String title, String description) {
        this(); // da bi se cons za incijijalizaciju tecaja pravilno pozvao
        this.title = title;
        this.description = description;
        this.courseStatus = CourseStatus.DRAFT;
    }

    // Factory metoda - static jer stvara novi objekt
    public static Course create(CreateCourseRequest createCourseRequest) {
        return new Course(createCourseRequest.title(), createCourseRequest.description());
    }

    // Domenski važne metode ostaju u entitetu

    public void addModule(CourseModule courseModule){
        log.info("Adding module to course: {}", this.Id);
        validateModuleAddition(courseModule);
        log.info("Validation passed for module: {}", courseModule);

        courseModule.setCourse(this);
        modules.add(courseModule);
        log.info("Module added to course. Total modules: {}", this.modules.size());

        statistics.recalculate(modules);
        log.info("Statistics recalculated: totalModules={}, totalDuration={}",
                statistics.getTotalModules(), statistics.getTotalDuration());

        // Create a snapshot
     //   CourseStatisticHistory snapshot = createSnap();
      //  log.info("Snapshot created: {}", snapshot);


    }


    public void publish() {
        if (!courseStatus.canTransitionTo(CourseStatus.PUBLISHED)) {
            throw new IllegalStateException("Course can only be published from DRAFT state");
        }
        courseStatus = CourseStatus.PUBLISHED;
    }

    private void validateModuleAddition(CourseModule module) {
        if (!courseStatus.allowsModification()) {
            throw new IllegalStateException("Can only add modules to courses in DRAFT status");
        }
        Objects.requireNonNull(module, "Module cannot be null");
    }

    // history
    public CourseStatisticHistory createSnap() {
        log.debug("Creating snapshot for course ID: {}", this.Id);

        CourseStatisticHistory snapShot = CourseStatisticHistory.createSnapshot(this);
        statisticHistory.add(snapShot);
        log.debug("Snapshot added to course statistic history. Total snapshots: {}", statisticHistory.size());

        return snapShot;

    }

  //  dohvat povijesti statistika za određeni period

    public List<CourseStatisticHistory> getStatisticHistory(LocalDateTime startDate, LocalDateTime endDate) {
        return statisticHistory.stream()
                .filter(history -> history.getSnapshotTimestamp().isAfter(startDate)
                && history.getSnapshotTimestamp().isBefore(endDate))
                .collect(Collectors.toList());
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return Objects.equals(Id, course.Id) && Objects.equals(title, course.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Id, title);
    }

     */
}
