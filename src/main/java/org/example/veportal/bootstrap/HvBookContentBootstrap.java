package org.example.veportal.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import org.example.veportal.config.AppProperties;
import org.example.veportal.entity.Chapter;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.CourseMaterial;
import org.example.veportal.entity.MaterialType;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.Topic;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.exception.BusinessException;
import org.example.veportal.repository.ChapterRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.example.veportal.repository.CourseRepository;
import org.example.veportal.repository.UserAccountRepository;
import org.example.veportal.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Keeps the bundled 3rd-edition UHV book usable as academic structure and reference material. */
@Component
public class HvBookContentBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(HvBookContentBootstrap.class);
    private static final String BOOK_FILE = "A Foundation Course in Human Values and Professional Ethics (eBook)_3rd Edition.pdf";
    private static final String BOOK_TITLE = "A Foundation Course in Human Values and Professional Ethics — 3rd Edition";

    private record ChapterSeed(String title, String description, List<String> topics) {}

    private static final List<ChapterSeed> CHAPTERS = List.of(
        c("Understanding Value Education", "Section I — purpose, scope and relevance of value education.", "Purpose and need for value education", "Content of value education", "Process of self-exploration", "Natural acceptance and experiential verification", "Role of education in human development", "Appendix A1-1: Typical course structure"),
        c("Self-exploration as the Process for Value Education", "Section I — self-exploration as a dialogue between a proposal, natural acceptance and living.", "Meaning and process of self-exploration", "Knowing and assuming", "Natural acceptance", "Experiential verification", "Self-exploration in daily living"),
        c("Basic Human Aspirations and their Fulfilment", "Section I — continuous happiness and prosperity as basic human aspirations and their fulfilment.", "Basic human aspirations", "Happiness and prosperity", "Right understanding, relationship and physical facility", "Programme for fulfilment", "Priority and balance among the three", "Appendix A3-1: What is basic aspiration?"),
        c("Understanding Happiness and Prosperity — Their Continuity and Programme for Fulfilment", "Section I — understanding happiness, prosperity, continuity and the programme for fulfilment.", "Happiness as harmony", "Prosperity and assessment of physical facilities", "Continuity of happiness and prosperity", "Right understanding and relationships", "Responsible use of physical facilities"),
        c("Understanding the Human Being as Co-existence of the Self and the Body", "Section II — the human being as co-existence of the conscious self and the material body.", "Self (I) and body", "Needs of the self and needs of the body", "Activities of the self", "Activities of the body", "Co-existence and their distinct roles"),
        c("Harmony in the Self — Understanding Myself", "Section II — understanding the self, its activities and inner harmony.", "Desire, thought and expectation", "Imagination and preconditioning", "Knowing, assuming, recognising and fulfilling", "Harmony in the self", "Self-evaluation and transformation", "Appendix A6-1: Activities of the self", "Appendix A6-2: Sanskar"),
        c("Harmony of the Self with the Body — Understanding Self-regulation and Health", "Section II — self-regulation, health and responsible care of the body.", "Self-regulation and sanyama", "Health and sv-asthya", "Understanding the requirements of the body", "Food, routine and physical work", "Prosperity in relation to the body"),
        c("Harmony in the Family — Understanding Values in Human-Human Relationships", "Section II — values, trust and justice in mutually fulfilling human relationships.", "Family as the basic unit of human interaction", "Trust and respect", "Affection, care and guidance", "Reverence, glory and gratitude", "Love, justice and mutual fulfilment"),
        c("Harmony in the Society — Understanding Universal Human Order", "Section II — extending harmony from family to undivided society and universal human order.", "From family order to society", "Justice and fearlessness", "Undivided society", "Universal human order", "Education, health, production and exchange", "Human goal and social participation", "Appendix A9-1: Process of understanding"),
        c("Harmony in Nature — Understanding the Interconnectedness, Self-regulation and Mutual Fulfilment", "Section II — the four orders of nature, cyclicity, interconnectedness and mutual fulfilment.", "Four orders of nature", "Material, plant, animal and human orders", "Interconnectedness and cyclic processes", "Self-regulation in nature", "Mutual fulfilment", "Human participation in nature"),
        c("Harmony in Existence — Understanding Co-existence at Various Levels", "Section II — existence as co-existence and an interconnected whole.", "Existence and units", "Space and units", "Co-existence at various levels", "Holistic view of reality", "Implications for human conduct"),
        c("The Basis for Universal Human Values and Ethical Human Conduct", "Section III — right understanding, universal human values and ethical human conduct.", "Universal human values", "Right understanding and ethical conduct", "Competence and ethical responsibility", "Defining and evaluating human conduct", "Professional accountability and long-term consequences", "Appendix A12-1: Universal human values"),
        c("Professional Ethics in the light of Right Understanding", "Section III — professional ethics grounded in right understanding and human welfare.", "Meaning and need of professional ethics", "Values and competence in professional life", "Ethical decision-making", "Technology and human consequences", "Responsibility towards society and nature", "Professional integrity"),
        c("Holistic Development towards Universal Human Order", "Section III — education, social systems and development oriented to universal human order.", "Holistic development", "Education and sanskar", "Health and self-regulation", "Justice and preservation", "Production and work", "Exchange, storage and social organisation"),
        c("Vision for Holistic Technologies, Production Systems and Management Models", "Section III — humane technology, production and management models.", "Holistic criteria for technology", "Appropriate and people-friendly production", "Cyclic and mutually fulfilling production", "Management through trust and participation", "Sustainable use of resources", "Evaluation of existing systems"),
        c("Journey towards Universal Human Order — The Road Ahead", "Section III — translating right understanding into individual, institutional and social action.", "From understanding to living", "Individual transformation", "Family and institutional responsibility", "Professional action plan", "Collective movement towards universal human order", "The road ahead", "Appendix P-2: UHV faculty development programs", "Appendix P-3: Minor degree in Universal Human Values", "Appendix P-4: UHV contribution to UGC efforts for values", "Appendix P-1: Efforts for UHV, 1980–2023", "Glossary and references")
    );

    private static ChapterSeed c(String title, String description, String... topics) {
        return new ChapterSeed(title, description, List.of(topics));
    }

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final TopicRepository topicRepository;
    private final CourseMaterialRepository materialRepository;
    private final UserAccountRepository userRepository;
    private final AppProperties properties;

    public HvBookContentBootstrap(CourseRepository courseRepository, ChapterRepository chapterRepository,
                                  TopicRepository topicRepository, CourseMaterialRepository materialRepository,
                                  UserAccountRepository userRepository, AppProperties properties) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.topicRepository = topicRepository;
        this.materialRepository = materialRepository;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Course> courses = courseRepository.findAll().stream()
                .filter(c -> "VE1".equalsIgnoreCase(c.getCourseType()) || "VE Course".equalsIgnoreCase(c.getName()))
                .toList();
        for (Course course : courses) {
            syncCourse(course);
        }
    }

    private void syncCourse(Course course) {
        for (int i = 0; i < CHAPTERS.size(); i++) {
            ChapterSeed seed = CHAPTERS.get(i);
            Chapter chapter = chapterRepository.findByCourseIdAndChapterNumber(course.getId(), i + 1)
                    .orElseGet(Chapter::new);
            chapter.setCourse(course);
            chapter.setChapterNumber(i + 1);
            chapter.setTitle(seed.title());
            chapter.setDescription(seed.description());
            chapter.setDisplayOrder(i + 1);
            chapter.setStatus("ACTIVE");
            chapter = chapterRepository.save(chapter);
            for (int j = 0; j < seed.topics().size(); j++) {
                String title = seed.topics().get(j);
                Topic topic = topicRepository.findByChapterIdOrderByDisplayOrderAscIdAsc(chapter.getId()).stream()
                        .filter(t -> title.equalsIgnoreCase(t.getTitle())).findFirst().orElseGet(Topic::new);
                topic.setChapter(chapter);
                topic.setTitle(title);
                topic.setDescription("Study focus: " + title + ". Connect the proposal with natural acceptance, understanding and practice.");
                topic.setDisplayOrder(j + 1);
                topic.setStatus("ACTIVE");
                topicRepository.save(topic);
            }
        }
        syncBookMaterial(course);
    }

    private void syncBookMaterial(Course course) {
        ClassPathResource source = new ClassPathResource(BOOK_FILE);
        if (!source.exists()) {
            log.warn("Bundled UHV book was not found on the classpath: {}", BOOK_FILE);
            return;
        }
        UserAccount uploader = course.getFaculty();
        if (uploader == null) {
            uploader = userRepository.findAllByRoleOrderByFullNameAsc(Role.FACULTY_COORDINATOR).stream().findFirst().orElse(null);
        }
        if (uploader == null) {
            log.warn("Skipping UHV book material because no uploader account exists");
            return;
        }
        String key = "hv-foundation-course-3rd-edition.pdf";
        Path directory = Path.of(properties.storage().materialsDir()).toAbsolutePath().normalize();
        Path target = directory.resolve(key).normalize();
        try {
            Files.createDirectories(directory);
            try (InputStream input = source.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException("Unable to install the bundled UHV book material");
        }
        CourseMaterial material = materialRepository.findFirstByCourseIdAndStorageKey(course.getId(), key)
                .orElseGet(CourseMaterial::new);
        material.setCourse(course);
        material.setTitle(BOOK_TITLE);
        material.setType(MaterialType.REFERENCE_PDF);
        material.setDescription("Official 3rd-edition reference eBook. Its 16 chapters, sections and appendices are mapped into the course curriculum.");
        material.setFileName(BOOK_FILE);
        try {
            material.setSizeBytes(Files.size(target));
        } catch (IOException e) {
            throw new BusinessException("Unable to inspect the bundled UHV book material");
        }
        material.setStorageKey(key);
        material.setContentType("application/pdf");
        material.setUploadedBy(uploader);
        material.setUploadedAt(LocalDate.now());
        materialRepository.save(material);
    }
}
