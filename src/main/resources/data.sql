-- ============================================================
-- Development/Test seed data
-- Default passwords (all users): test123 
-- BCrypt hash for "test123" 
-- $2y$10$SewDwseJbGLx4sFCYGmYt.ic0zx1hvzbuEvF6jLwkFTgX3rcms7hi use job_portal; 
-- ============================================================

-- ============================================================
-- 1) USERS
-- ============================================================
INSERT IGNORE INTO user (id, email, password, enabled, created_at) VALUES
(1,  'admin@jobportal.com',      '$2y$10$SewDwseJbGLx4sFCYGmYt.ic0zx1hvzbuEvF6jLwkFTgX3rcms7hi', true,  '2026-01-01 08:00:00'),

-- Employers
(2,  'alice.employer@acme.com',  '$2y$10$SewDwseJbGLx4sFCYGmYt.ic0zx1hvzbuEvF6jLwkFTgX3rcms7hi', true,  '2026-01-05 09:00:00'),
(3,  'bob.employer@nexus.com',   '$2y$10$SewDwseJbGLx4sFCYGmYt.ic0zx1hvzbuEvF6jLwkFTgX3rcms7hi', true,  '2026-01-06 10:00:00'),
(4,  'carol.employer@wave.com',  '$2y$10$SewDwseJbGLx4sFCYGmYt.ic0zx1hvzbuEvF6jLwkFTgX3rcms7hi', false, '2026-01-07 11:00:00'),

-- Candidates
(5,  'john.doe@mail.com',        '$2y$10$SewDwseJbGLx4sFCYGmYt.ic0zx1hvzbuEvF6jLwkFTgX3rcms7hi', true,  '2026-01-10 08:30:00'),
(6,  'jane.smith@mail.com',      '$2y$10$SewDwseJbGLx4sFCYGmYt.ic0zx1hvzbuEvF6jLwkFTgX3rcms7hi', true,  '2026-01-11 09:15:00'),
(7,  'mike.jones@mail.com',      '$2y$10$SewDwseJbGLx4sFCYGmYt.ic0zx1hvzbuEvF6jLwkFTgX3rcms7hi', true,  '2026-01-12 10:45:00'),
(8,  'sara.lee@mail.com',        '$2y$10$SewDwseJbGLx4sFCYGmYt.ic0zx1hvzbuEvF6jLwkFTgX3rcms7hi', true,  '2026-01-13 11:00:00'),
(9,  'tom.white@mail.com',       '$2y$10$SewDwseJbGLx4sFCYGmYt.ic0zx1hvzbuEvF6jLwkFTgX3rcms7hi', false, '2026-01-14 12:00:00');


-- ============================================================
-- 2) ROLES
-- ============================================================
INSERT IGNORE INTO user_roles (user_id, roles) VALUES
(1, 'ADMIN'),
(2, 'EMPLOYER'),
(3, 'EMPLOYER'),
(4, 'EMPLOYER'),
(5, 'CANDIDATE'),
(6, 'CANDIDATE'),
(7, 'CANDIDATE'),
(8, 'CANDIDATE'),
(9, 'CANDIDATE');


-- ============================================================
-- 3) EMPLOYERS
-- ============================================================
INSERT IGNORE INTO employer (id, company_name, website, user_id) VALUES
(1, 'Acme Tech',      'https://acme.example',  2),
(2, 'Nexus Solutions','https://nexus.example', 3),
(3, 'Wave Digital',   'https://wave.example',  4);


-- ============================================================
-- 4) CANDIDATES
-- ============================================================
INSERT IGNORE INTO candidate (id, full_name, resume_url, user_id) VALUES
(1, 'John Doe',    'https://cdn.example/resumes/john-doe.pdf',    5),
(2, 'Jane Smith',  'https://cdn.example/resumes/jane-smith.pdf',  6),
(3, 'Mike Jones',  'https://cdn.example/resumes/mike-jones.pdf',  7),
(4, 'Sara Lee',    'https://cdn.example/resumes/sara-lee.pdf',    8),
(5, 'Tom White',   'https://cdn.example/resumes/tom-white.pdf',   9);


-- ============================================================
-- 5) JOBS
-- ============================================================
INSERT IGNORE INTO jobs (id, closing_date, created_at, description, job_type, location, salary, status, title, employer_id) VALUES

-- Acme Tech (employer 1)
(1,  '2027-03-01', '2026-01-15 09:00:00', 'Build and maintain REST APIs using Spring Boot and microservices.',           'FULL_TIME',  'Yerevan',      90000, 'ACTIVE',  'Java Backend Engineer',        1),
(2,  '2027-04-01', '2026-01-18 14:00:00', 'Manual and automation testing for web and mobile applications.',              'FULL_TIME',  'Remote',       65000, 'ACTIVE',  'QA Engineer',                  1),
(3,  '2027-05-01', '2026-02-01 10:00:00', 'Design and implement scalable cloud infrastructure on AWS.',                  'FULL_TIME',  'Yerevan',     110000, 'ACTIVE',  'Cloud Infrastructure Engineer',1),
(4,  '2027-06-01', '2026-02-10 11:00:00', 'Manage and mentor a team of backend engineers across two product squads.',    'FULL_TIME',  'Yerevan',     130000, 'ACTIVE',  'Engineering Manager',          1),
(5,  '2026-04-01', '2025-12-01 09:00:00', 'Support and maintain legacy ERP platform services.',                          'CONTRACT',   'Gyumri',       55000, 'CLOSED',  'Legacy Support Specialist',    1),
(6,  '2025-06-01', '2025-01-10 09:30:00', 'Develop UI components and pages using React and TypeScript.',                 'FULL_TIME',  'Yerevan',      70000, 'EXPIRED', 'Frontend Developer',            1),

-- Nexus Solutions (employer 2)
(7,  '2027-03-15', '2026-01-20 10:00:00', 'Analyze business requirements and translate them into technical specs.',      'FULL_TIME',  'Yerevan',      75000, 'ACTIVE',  'Business Analyst',             2),
(8,  '2027-04-10', '2026-01-25 09:30:00', 'Design user-friendly interfaces for our SaaS product suite.',                 'FULL_TIME',  'Remote',       80000, 'ACTIVE',  'UI/UX Designer',               2),
(9,  '2027-05-20', '2026-02-05 14:00:00', 'Maintain and optimize PostgreSQL and MySQL databases in production.',         'PART_TIME',  'Remote',       60000, 'ACTIVE',  'Database Administrator',       2),
(10, '2026-04-15', '2025-11-01 08:00:00', 'Lead DevOps transformation and implement CI/CD pipelines.',                   'FULL_TIME',  'Yerevan',     100000, 'CLOSED',  'DevOps Lead',                  2),
(11, '2025-09-01', '2025-03-01 09:00:00', 'Part-time data entry and reporting support role.',                            'PART_TIME',  'Gyumri',       30000, 'EXPIRED', 'Data Entry Specialist',        2),

-- Wave Digital (employer 3) — disabled employer, jobs still visible
(12, '2027-06-01', '2026-02-15 11:00:00', 'Develop and maintain Android and iOS apps using Flutter.',                   'FULL_TIME',  'Remote',       85000, 'ACTIVE',  'Mobile Developer',             3),
(13, '2025-12-01', '2025-06-01 10:00:00', 'Provide IT helpdesk support for internal teams.',                            'CONTRACT',   'Vanadzor',     40000, 'EXPIRED', 'IT Support Specialist',        3);


-- ============================================================
-- 6) APPLICATIONS
-- ============================================================
INSERT IGNORE INTO applications (id, applied_at, cover_letter, status, candidate_id, job_id) VALUES

-- John Doe (candidate 1)
(1,  '2026-01-20 10:00:00', 'I have 4 years of Spring Boot experience and love building clean APIs.',        'APPLIED',    1, 1),
(2,  '2026-01-22 11:00:00', 'I am highly interested in cloud infrastructure and hold an AWS cert.',          'REVIEWED',   1, 3),
(3,  '2026-01-25 14:00:00', 'I would love to grow into a management role at Acme Tech.',                     'ACCEPTED',   1, 4),
(4,  '2026-01-28 09:30:00', 'I have strong analytical skills and experience with SaaS products.',            'REJECTED',   1, 7),

-- Jane Smith (candidate 2)
(5,  '2026-01-21 09:00:00', 'I have 3 years of QA experience including Selenium and Cypress.',               'APPLIED',    2, 2),
(6,  '2026-01-23 10:30:00', 'UI/UX is my passion. I have a strong Figma and design systems portfolio.',      'REVIEWED',   2, 8),
(7,  '2026-01-26 15:00:00', 'I am excited to contribute to Nexus as a business analyst.',                    'ACCEPTED',   2, 7),
(8,  '2026-02-01 11:00:00', 'I have solid Flutter experience with 2 published apps.',                        'CANCELLED',  2, 12),

-- Mike Jones (candidate 3)
(9,  '2026-01-22 08:00:00', 'Database performance tuning is my core strength.',                              'APPLIED',    3, 9),
(10, '2026-01-24 13:00:00', 'I have led DevOps teams and built pipelines from scratch.',                     'REVIEWED',   3, 10),
(11, '2026-01-27 16:00:00', 'I am confident I can add value to the cloud infrastructure team.',              'REJECTED',   3, 3),

-- Sara Lee (candidate 4)
(12, '2026-01-23 10:00:00', 'I am a fast learner eager to start my QA career.',                              'APPLIED',    4, 2),
(13, '2026-01-25 11:30:00', 'I have a background in mobile development and am passionate about Flutter.',    'REVIEWED',   4, 12),
(14, '2026-01-29 09:00:00', 'I would love to join the backend engineering team.',                            'ACCEPTED',   4, 1),

-- Tom White (candidate 5 — disabled user, applications still in system)
(15, '2026-01-24 14:00:00', 'I am interested in the engineering manager position.',                          'APPLIED',    5, 4),
(16, '2026-01-26 10:00:00', 'I have DBA experience across Oracle and MySQL environments.',                   'REJECTED',   5, 9);