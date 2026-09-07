package org.example.veportal.bootstrap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import org.example.veportal.config.AppProperties;
import org.example.veportal.entity.AccountStatus;
import org.example.veportal.entity.AcademicYear;
import org.example.veportal.entity.ActivityKind;
import org.example.veportal.entity.AttendanceRecord;
import org.example.veportal.entity.AttendanceStatus;
import org.example.veportal.entity.Chapter;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.entity.Course;
import org.example.veportal.entity.CourseFaculty;
import org.example.veportal.entity.CourseMaterial;
import org.example.veportal.entity.CourseStudent;
import org.example.veportal.entity.MaterialType;
import org.example.veportal.entity.Notification;
import org.example.veportal.entity.ParticipationLevel;
import org.example.veportal.entity.ParticipationRecord;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.SessionStatus;
import org.example.veportal.entity.Student;
import org.example.veportal.entity.TeachingLog;
import org.example.veportal.entity.TeachingLogConcept;
import org.example.veportal.entity.UserAccount;
import org.example.veportal.repository.AcademicYearRepository;
import org.example.veportal.repository.ActivityLogRepository;
import org.example.veportal.repository.AttendanceRecordRepository;
import org.example.veportal.repository.ChapterRepository;
import org.example.veportal.repository.ClassSessionRepository;
import org.example.veportal.repository.CourseFacultyRepository;
import org.example.veportal.repository.CourseMaterialRepository;
import org.example.veportal.repository.CourseRepository;
import org.example.veportal.repository.CourseStudentRepository;
import org.example.veportal.repository.NotificationRepository;
import org.example.veportal.repository.ParticipationRecordRepository;
import org.example.veportal.repository.StudentRepository;
import org.example.veportal.repository.TeachingLogRepository;
import org.example.veportal.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "Passw0rd!";

    private record Topic(String topic, String date, SessionStatus status) {
    }

    private static final String[] FIRST_NAMES = {
            "Rahul", "Ananya", "Vihaan", "Ishita", "Arjun", "Meera", "Kabir", "Sana",
            "Aditya", "Diya", "Rohan", "Nisha", "Aarav", "Pooja", "Dev", "Tanvi",
            "Karthik", "Aisha", "Nikhil", "Shreya", "Varun", "Lakshmi", "Siddharth", "Riya",
            "Harsh", "Anjali", "Yash", "Kavya", "Manav", "Sneha", "Ishan", "Priya",
            "Vivek", "Neha", "Abhinav", "Divya"
    };
    private static final String[] LAST_NAMES = {
            "Sharma", "Rao", "Iyer", "Reddy", "Nair", "Patel", "Gupta", "Mehta",
            "Kulkarni", "Joshi", "Singh", "Das", "Menon", "Khan", "Pillai", "Deshmukh"
    };
    private static final String[] PROGRAMMES = {
            "B.Tech CSE", "B.Tech ECE", "Dual Degree CSE", "M.Tech CSE", "MS by Research"
    };

    private static final List<Topic> TOPICS = List.of(
            new Topic("Course Orientation and VE Operating Model", "2026-01-09", SessionStatus.COMPLETED),
            new Topic("Problem Framing for Virtual Enterprises", "2026-01-16", SessionStatus.COMPLETED),
            new Topic("Stakeholder Mapping and Requirements", "2026-01-23", SessionStatus.COMPLETED),
            new Topic("Data Structures — Foundations", "2026-01-30", SessionStatus.COMPLETED),
            new Topic("Complexity and Algorithmic Thinking", "2026-02-06", SessionStatus.COMPLETED),
            new Topic("Linear Structures in Practice", "2026-02-13", SessionStatus.COMPLETED),
            new Topic("Trees and Hierarchical Data", "2026-02-20", SessionStatus.COMPLETED),
            new Topic("Hashing and Lookup Design", "2026-02-27", SessionStatus.COMPLETED),
            new Topic("Sorting Strategies and Trade-offs", "2026-03-06", SessionStatus.COMPLETED),
            new Topic("Graph Representations", "2026-03-13", SessionStatus.COMPLETED),
            new Topic("BFS, DFS and Traversal Patterns", "2026-03-20", SessionStatus.COMPLETED),
            new Topic("Shortest Paths in Networks", "2026-04-03", SessionStatus.COMPLETED),
            new Topic("Spanning Trees and Connectivity", "2026-04-10", SessionStatus.COMPLETED),
            new Topic("Greedy Design Patterns", "2026-04-17", SessionStatus.COMPLETED),
            new Topic("Dynamic Programming — Foundations", "2026-07-24", SessionStatus.COMPLETED),
            new Topic("Recursion and Backtracking", "2026-07-31", SessionStatus.COMPLETED),
            new Topic("Algorithmic Problem Clinic", "2026-08-07", SessionStatus.COMPLETED),
            new Topic("Advanced Graph Algorithms", "2026-08-14", SessionStatus.COMPLETED),
            new Topic("Data Structures — Advanced Topics", "2026-08-21", SessionStatus.UPCOMING),
            new Topic("Capstone Briefing", "2026-08-28", SessionStatus.DRAFT)
    );

    private static final Map<Integer, String[]> SPECIAL_LOGS = Map.of(
            1, new String[]{
                    "Introduced the VE Course structure, faculty expectations, and how class documentation will be maintained. Walked through attendance, materials, and the teaching log workflow.",
                    "Course operating model|Faculty documentation workflow|Session cadence",
                    "Students asked about session recordings; clarified that this portal is faculty-facing only."},
            4, new String[]{
                    "Covered abstract data types, arrays versus lists, and when to choose each structure in enterprise-style problems.",
                    "ADT|Arrays|Linked lists|Access patterns",
                    "Used a short VE inventory example to ground the discussion."},
            10, new String[]{
                    "Introduced adjacency lists and matrices, and discussed modelling campus and organisation networks as graphs.",
                    "Graph representations|Adjacency list|Adjacency matrix",
                    "Assigned a short reading on sparse graphs."},
            17, new String[]{
                    "Ran a problem clinic on previously taught structures. Students worked through two VE-inspired design prompts and presented trade-offs.",
                    "Problem decomposition|Trade-off analysis",
                    "Stronger engagement than last week. Follow up on hashing edge cases next class."}
    );
    private static final String DEFAULT_LOG_CONTENT =
            "Reviewed the planned topic, worked through representative examples, and summarised design implications for VE coursework.";
    private static final String DEFAULT_LOG_CONCEPTS = "Core definitions|Worked examples|Design implications";
    private static final String DEFAULT_LOG_REMARKS = "Session completed as scheduled.";

    private record MaterialSeed(String title, MaterialType type, Integer sessionNumber,
                                String uploadedBy, String uploadedAt, String size, String description) {
    }

    private static final List<MaterialSeed> MATERIALS = List.of(
            new MaterialSeed("VE Course Handbook", MaterialType.REFERENCE_PDF, 1, "faculty", "2026-01-08", "1.8 MB", "Faculty reference for course operations and session documentation."),
            new MaterialSeed("Orientation Slides", MaterialType.SLIDES, 1, "faculty", "2026-01-09", "4.2 MB", "Opening session slides covering course structure."),
            new MaterialSeed("Problem Framing Notes", MaterialType.LECTURE_NOTES, 2, "faculty", "2026-01-16", "620 KB", "Notes on framing VE problems with stakeholders."),
            new MaterialSeed("Requirements Worksheet", MaterialType.ASSIGNMENT_BRIEF, 3, "faculty", "2026-01-23", "210 KB", "Short in-class worksheet on stakeholder requirements."),
            new MaterialSeed("Data Structures Foundations", MaterialType.LECTURE_NOTES, 4, "faculty", "2026-01-30", "1.1 MB", "Lecture notes on ADTs, arrays and lists."),
            new MaterialSeed("Foundations Slides", MaterialType.SLIDES, 4, "faculty", "2026-01-30", "3.4 MB", "Slide deck for the foundations session."),
            new MaterialSeed("Complexity Primer", MaterialType.READING_MATERIAL, 5, "faculty", "2026-02-05", "880 KB", "Reading on time and space trade-offs."),
            new MaterialSeed("Linear Structures Lab Brief", MaterialType.ASSIGNMENT_BRIEF, 6, "faculty", "2026-02-13", "340 KB", "Brief for in-class linear structure exercises."),
            new MaterialSeed("Trees and Hierarchies", MaterialType.LECTURE_NOTES, 7, "faculty", "2026-02-20", "970 KB", "Notes on trees in organisational data."),
            new MaterialSeed("Hashing Design Notes", MaterialType.LECTURE_NOTES, 8, "faculty", "2026-02-27", "740 KB", "Collision handling and lookup design."),
            new MaterialSeed("Sorting Strategies Slides", MaterialType.SLIDES, 9, "faculty", "2026-03-06", "2.9 MB", "Comparison of common sorting strategies."),
            new MaterialSeed("Graph Representations", MaterialType.LECTURE_NOTES, 10, "faculty", "2026-03-13", "1.3 MB", "Adjacency structures and modelling notes."),
            new MaterialSeed("Sparse Graphs Reading", MaterialType.READING_MATERIAL, 10, "faculty", "2026-03-14", "540 KB", "Supplementary reading on sparse graphs."),
            new MaterialSeed("Traversal Patterns Slides", MaterialType.SLIDES, 11, "faculty", "2026-03-20", "3.1 MB", "BFS and DFS walkthroughs."),
            new MaterialSeed("Shortest Paths Notes", MaterialType.LECTURE_NOTES, 12, "faculty", "2026-04-03", "1.0 MB", "Dijkstra and related path algorithms."),
            new MaterialSeed("Connectivity Exercises", MaterialType.ASSIGNMENT_BRIEF, 13, "faculty", "2026-04-10", "280 KB", "In-class connectivity exercises."),
            new MaterialSeed("Greedy Patterns Notes", MaterialType.LECTURE_NOTES, 14, "faculty", "2026-04-17", "690 KB", "When greedy methods are appropriate."),
            new MaterialSeed("DP Foundations Slides", MaterialType.SLIDES, 15, "faculty", "2026-07-24", "3.6 MB", "Introductory dynamic programming slides."),
            new MaterialSeed("DP Worked Examples", MaterialType.LECTURE_NOTES, 15, "faculty", "2026-07-24", "820 KB", "Two worked VE-inspired DP examples."),
            new MaterialSeed("Recursion Clinic Brief", MaterialType.ASSIGNMENT_BRIEF, 16, "faculty", "2026-07-31", "190 KB", "Backtracking prompts used in class."),
            new MaterialSeed("Problem Clinic Pack", MaterialType.LECTURE_NOTES, 17, "faculty", "2026-08-07", "1.4 MB", "Problems discussed in the clinic session."),
            new MaterialSeed("Advanced Graph Algorithms", MaterialType.LECTURE_NOTES, 18, "faculty", "2026-08-14", "1.6 MB", "SCC, topological ideas and advanced traversals."),
            new MaterialSeed("Graph Algorithms Slides", MaterialType.SLIDES, 18, "faculty", "2026-08-14", "4.0 MB", "Session 18 slide deck."),
            new MaterialSeed("CLRS Graph Chapters (excerpt)", MaterialType.READING_MATERIAL, 18, "faculty", "2026-08-13", "2.2 MB", "Selected reading ahead of Session 18."),
            new MaterialSeed("Faculty Session Checklist", MaterialType.REFERENCE_PDF, null, "faculty", "2026-01-06", "150 KB", "Attendance, materials and teaching-log checklist."),
            new MaterialSeed("IIIT-H Academic Calendar Extract", MaterialType.REFERENCE_PDF, null, "office", "2026-01-02", "430 KB", "Relevant dates for Spring 2026 VE sessions."),
            new MaterialSeed("Upcoming Session Outline", MaterialType.LECTURE_NOTES, 19, "faculty", "2026-08-19", "260 KB", "Draft outline for advanced topics session.")
    );

    private final AppProperties appProperties;
    private final StudentRepository studentRepository;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final ParticipationRecordRepository participationRepository;
    private final TeachingLogRepository teachingLogRepository;
    private final CourseMaterialRepository materialRepository;
    private final CourseRepository courseRepository;
    private final UserAccountRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ChapterRepository chapterRepository;
    private final CourseFacultyRepository courseFacultyRepository;
    private final CourseStudentRepository courseStudentRepository;
    private final PasswordEncoder passwordEncoder;
    private final jakarta.persistence.EntityManager entityManager;

    public DemoDataSeeder(AppProperties appProperties,
                          StudentRepository studentRepository,
                          ClassSessionRepository sessionRepository,
                          AttendanceRecordRepository attendanceRepository,
                          ParticipationRecordRepository participationRepository,
                          TeachingLogRepository teachingLogRepository,
                          CourseMaterialRepository materialRepository,
                          CourseRepository courseRepository,
                          UserAccountRepository userRepository,
                          NotificationRepository notificationRepository,
                          ActivityLogRepository activityLogRepository,
                          AcademicYearRepository academicYearRepository,
                          ChapterRepository chapterRepository,
                          CourseFacultyRepository courseFacultyRepository,
                          CourseStudentRepository courseStudentRepository,
                          PasswordEncoder passwordEncoder,
                          jakarta.persistence.EntityManager entityManager) {
        this.appProperties = appProperties;
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.participationRepository = participationRepository;
        this.teachingLogRepository = teachingLogRepository;
        this.materialRepository = materialRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.activityLogRepository = activityLogRepository;
        this.academicYearRepository = academicYearRepository;
        this.chapterRepository = chapterRepository;
        this.courseFacultyRepository = courseFacultyRepository;
        this.courseStudentRepository = courseStudentRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!appProperties.seedDemoData()) {
            return;
        }
        log.info("Seeding VE Portal demo data...");
        initialiseReferencePasswords();

        Course course = courseRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Course reference data missing"));
        UserAccount faculty = userRepository.findByEmailIgnoreCase("harshul.dondeti@iiit.ac.in")
                .orElseThrow(() -> new IllegalStateException("Faculty reference data missing"));
        UserAccount office = userRepository.findByEmailIgnoreCase("admin@iiit.ac.in")
                .orElseThrow(() -> new IllegalStateException("Admin reference data missing"));

        seedAcademicStructure(course, faculty);

        if (studentRepository.count() > 0 || sessionRepository.count() > 0) {
            log.info("Existing data found; skipping demo data population.");
            return;
        }

        Lcg studentsRandom = new Lcg(42);
        List<Student> students = seedStudents(studentsRandom);
        seedStudentEnrollments(course, students);
        List<ClassSession> sessions = seedSessions(course, faculty);
        seedAttendance(sessions, students);
        seedParticipation(sessions, students);
        seedTeachingLogs(sessions, faculty);
        seedMaterials(course, sessions, faculty, office);

        LocalDateTime base = LocalDateTime.now();
        seedNotification(faculty, "Teaching log incomplete",
                "Session 18 — Advanced Graph Algorithms does not yet have a teaching log.",
                "/teaching-log?session=" + sessionIdFor(sessions, 18), base.minusMinutes(95));
        seedNotification(faculty, "Next class tomorrow",
                "Session 19 is scheduled Friday, 21 Aug, 10:00 AM in LH-1.",
                "/classes/" + sessionIdFor(sessions, 19), base.minusMinutes(110));
        seedNotification(faculty, "New material uploaded",
                "Upcoming Session Outline was added to Course Materials.",
                "/materials", base.minusDays(1).withHour(17));
        seedNotification(faculty, "Session record updated",
                "Attendance for Session 18 was saved.",
                "/attendance?session=" + sessionIdFor(sessions, 18), base.minusDays(10));

        ClassSession session18 = sessionForNumber(sessions, 18);
        ClassSession session17 = sessionForNumber(sessions, 17);
        ClassSession session16 = sessionForNumber(sessions, 16);
        seedActivity(faculty, ActivityKind.ATTENDANCE,
                "Attendance recorded for Session 18",
                "Advanced Graph Algorithms · 91% present",
                "/attendance?session=" + session18.getId(), base.withHour(11).withMinute(42));
        seedActivity(faculty, ActivityKind.LOG,
                "Teaching log updated for Session 17",
                "Algorithmic Problem Clinic",
                "/teaching-log?session=" + session17.getId(), base.withHour(16).withMinute(40));
        seedActivity(faculty, ActivityKind.MATERIAL,
                "New course material uploaded",
                "Advanced Graph Algorithms — Lecture Notes",
                "/materials", base.withHour(9).withMinute(20));
        seedActivity(faculty, ActivityKind.PARTICIPATION,
                "Participation updated for Session 16",
                "Recursion and Backtracking",
                "/participation?session=" + session16.getId(), base.withHour(12).withMinute(5));

        log.info("Demo data seeded: {} students, {} sessions.", students.size(), sessions.size());
    }

    private void initialiseReferencePasswords() {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        entityManager.clear();
        for (String email : List.of("harshul.dondeti@iiit.ac.in", "admin@iiit.ac.in")) {
            int updated = entityManager.createQuery(
                            "update UserAccount u set u.passwordHash = :hash where u.email = :email")
                    .setParameter("hash", hash)
                    .setParameter("email", email)
                    .executeUpdate();
            if (updated == 0) {
                log.warn("Reference user {} was not found while initialising credentials", email);
            }
        }
        entityManager.flush();
    }

    private List<Student> seedStudents(Lcg random) {
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            String first = FIRST_NAMES[i % FIRST_NAMES.length];
            String last = LAST_NAMES[(i * 3) % LAST_NAMES.length];
            String name = first + " " + last;
            String year = switch (i % 3) {
                case 0 -> "2023";
                case 1 -> "2024";
                default -> "2025";
            };
            String code = year + String.format("%04d", 1100 + i) + "H";
            Student student = new Student();
            student.setStudentCode(code);
            student.setFullName(name);
            student.setProgramme(PROGRAMMES[i % PROGRAMMES.length]);
            student.setBatch(year);
            student.setStatus(random.next() > 0.04 ? AccountStatus.ACTIVE : AccountStatus.INACTIVE);
            student.setEmail((first + "." + last).toLowerCase() + i + "@students.iiit.ac.in");
            students.add(studentRepository.save(student));
        }
        return students;
    }

    private void seedAcademicStructure(Course course, UserAccount faculty) {
        AcademicYear year = seedAcademicYear();
        if (course.getAcademicYear() == null) {
            course.setAcademicYear(year);
            course = courseRepository.save(course);
        }
        seedChapters(course);
        seedFacultyMapping(course, faculty);
    }

    private AcademicYear seedAcademicYear() {
        if (academicYearRepository.count() > 0) {
            return academicYearRepository.findByCurrentTrue().orElseGet(
                    () -> academicYearRepository.findAll().stream().findFirst().orElseThrow());
        }
        AcademicYear year = new AcademicYear();
        year.setName("2025-2026");
        year.setStartYear(2025);
        year.setEndYear(2026);
        year.setCurrent(true);
        year.setStatus(AccountStatus.ACTIVE);
        return academicYearRepository.save(year);
    }

    private void seedChapters(Course course) {
        if (chapterRepository.countByCourseId(course.getId()) > 0) {
            return;
        }
        String[][] chapterData = {
                {"Course Orientation and VE Operating Model", "Overview of the VE course, its operating model, and documentation workflows."},
                {"Problem Framing for Virtual Enterprises", "How to frame problems, map stakeholders, and capture requirements."},
                {"Data Structures — Foundations", "Abstract data types, arrays, and lists in enterprise-style problems."},
                {"Complexity and Algorithmic Thinking", "Time and space complexity with algorithm design trade-offs."},
                {"Linear Structures in Practice", "Stacks, queues, and linked structures in applied settings."},
                {"Trees and Hierarchical Data", "Tree structures for organisational and hierarchical data."},
                {"Hashing and Lookup Design", "Hash tables, collision handling, and lookup optimisation."},
                {"Sorting Strategies and Trade-offs", "Comparison of common sorting strategies and their costs."},
                {"Graph Representations", "Adjacency lists and matrices for modelling networks."},
                {"BFS, DFS and Traversal Patterns", "Breadth-first and depth-first graph traversal."},
                {"Shortest Paths in Networks", "Dijkstra and related path-finding algorithms."},
                {"Spanning Trees and Connectivity", "Minimum spanning trees and connectivity analysis."},
                {"Greedy Design Patterns", "When greedy approaches are appropriate and their limitations."},
                {"Dynamic Programming — Foundations", "Introductory dynamic programming patterns and examples."},
                {"Recursion and Backtracking", "Recursive problem solving and backtracking techniques."},
                {"Advanced Graph Algorithms", "SCC, topological order and advanced traversal ideas."}
        };
        for (int i = 0; i < chapterData.length; i++) {
            Chapter chapter = new Chapter();
            chapter.setCourse(course);
            chapter.setChapterNumber(i + 1);
            chapter.setTitle(chapterData[i][0]);
            chapter.setDescription(chapterData[i][1]);
            chapter.setDisplayOrder(i + 1);
            chapter.setStatus("ACTIVE");
            chapterRepository.save(chapter);
        }
    }

    private void seedFacultyMapping(Course course, UserAccount faculty) {
        if (courseFacultyRepository.existsByCourseIdAndFacultyId(course.getId(), faculty.getId())) {
            return;
        }
        courseFacultyRepository.save(new CourseFaculty(course.getId(), faculty.getId()));
    }

    private void seedStudentEnrollments(Course course, List<Student> students) {
        if (courseStudentRepository.findByCourseId(course.getId()).isEmpty()) {
            for (Student student : students) {
                if (student.getStatus() == AccountStatus.ACTIVE) {
                    courseStudentRepository.save(new CourseStudent(course.getId(), student.getId()));
                }
            }
        }
    }

    private List<ClassSession> seedSessions(Course course, UserAccount faculty) {
        List<ClassSession> sessions = new ArrayList<>();
        for (int i = 0; i < TOPICS.size(); i++) {
            Topic topic = TOPICS.get(i);
            ClassSession session = new ClassSession();
            session.setCourse(course);
            session.setSessionNumber(i + 1);
            session.setTopic(topic.topic());
            session.setSessionDate(LocalDate.parse(topic.date()));
            session.setStartTime("10:00");
            session.setEndTime("11:30");
            session.setRoom(i % 2 == 0 ? "LH-1" : "Himalaya 105");
            session.setFaculty(faculty);
            session.setStatus(topic.status());
            sessions.add(sessionRepository.save(session));
        }
        return sessions;
    }

    private void seedAttendance(List<ClassSession> sessions, List<Student> students) {
        Lcg random = new Lcg(91);
        List<Student> active = students.stream()
                .filter(student -> student.getStatus() == AccountStatus.ACTIVE)
                .toList();
        List<AttendanceRecord> records = new ArrayList<>();
        for (ClassSession session : sessions) {
            if (session.getStatus() != SessionStatus.COMPLETED) {
                continue;
            }
            for (Student student : active) {
                AttendanceRecord record = new AttendanceRecord();
                record.setSession(session);
                record.setStudent(student);
                record.setStatus(random.next() > 0.086 ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT);
                records.add(record);
            }
        }
        attendanceRepository.saveAll(records);
    }

    private void seedParticipation(List<ClassSession> sessions, List<Student> students) {
        Lcg random = new Lcg(16);
        ParticipationLevel[] levels = {ParticipationLevel.LOW, ParticipationLevel.MODERATE, ParticipationLevel.HIGH};
        List<Student> active = students.stream()
                .filter(student -> student.getStatus() == AccountStatus.ACTIVE)
                .toList();
        List<ParticipationRecord> records = new ArrayList<>();
        for (ClassSession session : sessions) {
            boolean targeted = List.of(14, 16, 17).contains(session.getSessionNumber())
                    && session.getStatus() == SessionStatus.COMPLETED;
            if (!targeted) {
                continue;
            }
            for (Student student : active) {
                boolean skip = random.next() > 0.72;
                if (skip) {
                    continue;
                }
                ParticipationLevel level = levels[(int) Math.floor(random.next() * 3)];
                double noteRoll = random.next();
                ParticipationRecord record = new ParticipationRecord();
                record.setSession(session);
                record.setStudent(student);
                record.setLevel(level);
                record.setNotes(noteRoll > 0.7 ? "Asked a clarifying question during discussion." : "");
                records.add(record);
            }
        }
        participationRepository.saveAll(records);
    }

    private void seedTeachingLogs(List<ClassSession> sessions, UserAccount faculty) {
        for (ClassSession session : sessions) {
            if (session.getStatus() != SessionStatus.COMPLETED || session.getSessionNumber() == 18) {
                continue;
            }
            String[] special = SPECIAL_LOGS.get(session.getSessionNumber());
            String content = special != null ? special[0] : DEFAULT_LOG_CONTENT;
            String concepts = special != null ? special[1] : DEFAULT_LOG_CONCEPTS;
            String remarks = special != null ? special[2] : DEFAULT_LOG_REMARKS;
            TeachingLog logEntity = new TeachingLog();
            logEntity.setSession(session);
            logEntity.setContent(content);
            logEntity.setRemarks(remarks);
            logEntity.setUpdatedByUser(faculty);
            String[] conceptLabels = concepts.split("\\|");
            for (int position = 0; position < conceptLabels.length; position++) {
                TeachingLogConcept concept = new TeachingLogConcept();
                concept.setLog(logEntity);
                concept.setLabel(conceptLabels[position].trim());
                concept.setPosition(position);
                logEntity.getConcepts().add(concept);
            }
            teachingLogRepository.save(logEntity);
            entityManager.flush();
            entityManager.createQuery(
                            "update TeachingLog l set l.updatedAt = :updated where l.id = :id")
                    .setParameter("updated", LocalDate.parse(
                                    session.getSessionDate().toString()).atTime(16, 40))
                    .setParameter("id", logEntity.getId())
                    .executeUpdate();
        }
    }

    private void seedMaterials(Course course, List<ClassSession> sessions, UserAccount faculty, UserAccount office) {
        List<CourseMaterial> materials = new ArrayList<>();
        for (MaterialSeed seed : MATERIALS) {
            CourseMaterial material = new CourseMaterial();
            material.setCourse(course);
            material.setSession(seed.sessionNumber() == null
                    ? null
                    : sessionForNumber(sessions, seed.sessionNumber()));
            material.setTitle(seed.title());
            material.setType(seed.type());
            material.setDescription(seed.description());
            material.setSizeBytes(org.example.veportal.util.SizeFormat.parse(seed.size()));
            material.setUploadedBy("office".equals(seed.uploadedBy()) ? office : faculty);
            material.setUploadedAt(LocalDate.parse(seed.uploadedAt()));
            materials.add(material);
        }
        materialRepository.saveAll(materials);
    }

    private void seedNotification(UserAccount user, String title, String body, String href,
                                  LocalDateTime createdAt) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLinkUrl(href);
        notification.setReadAt(createdAt.isBefore(LocalDateTime.now().minusHours(30)) ? createdAt : null);
        Notification saved = notificationRepository.save(notification);
        alignCreatedAt(saved.getId(), "notifications", createdAt);
    }

    private void seedActivity(UserAccount user, ActivityKind kind, String title, String contextText,
                              String href, LocalDateTime createdAt) {
        org.example.veportal.entity.ActivityLog item = new org.example.veportal.entity.ActivityLog();
        item.setUser(user);
        item.setKind(kind);
        item.setTitle(title);
        item.setContextText(contextText);
        item.setHref(href);
        org.example.veportal.entity.ActivityLog saved = activityLogRepository.save(item);
        alignCreatedAt(saved.getId(), "activity_log", createdAt);
    }

    private void alignCreatedAt(Long id, String table, LocalDateTime createdAt) {
        entityManager.flush();
        entityManager.createNativeQuery("update " + table + " set created_at = :created where id = :id")
                .setParameter("created", createdAt)
                .setParameter("id", id)
                .executeUpdate();
    }

    private ClassSession sessionForNumber(List<ClassSession> sessions, int number) {
        return sessions.stream()
                .filter(session -> session.getSessionNumber() == number)
                .findFirst()
                .orElseThrow();
    }

    private String sessionIdFor(List<ClassSession> sessions, int number) {
        return sessionForNumber(sessions, number).getId().toString();
    }

    private static class Lcg implements DoubleSupplier {
        private long state;

        Lcg(long seed) {
            this.state = seed;
        }

        @Override
        public double getAsDouble() {
            state = (state * 16807) % 2147483647;
            if (state <= 0) {
                state += 2147483646;
            }
            return state / 2147483647.0;
        }

        double next() {
            return getAsDouble();
        }
    }
}
