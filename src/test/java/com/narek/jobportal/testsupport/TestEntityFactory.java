package com.narek.jobportal.testsupport;

import com.narek.jobportal.entity.*;

import java.util.Set;

public final class TestEntityFactory {

    private TestEntityFactory() {
    }

    public static User user(Long id, String email, boolean enabled, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("encoded");
        user.setEnabled(enabled);
        user.setRoles(Set.of(roles));
        return user;
    }

    public static Candidate candidate(Long id, User user) {
        Candidate candidate = new Candidate();
        candidate.setId(id);
        candidate.setFullName("Candidate " + id);
        candidate.setResumeUrl("https://resume.example/" + id);
        candidate.setUser(user);
        return candidate;
    }

    public static Employer employer(Long id, User user) {
        Employer employer = new Employer();
        employer.setId(id);
        employer.setCompanyName("Company " + id);
        employer.setWebsite("https://company" + id + ".example");
        employer.setUser(user);
        return employer;
    }

    public static Job job(Long id, Employer employer, double salary) {
        Job job = new Job();
        job.setId(id);
        job.setTitle("Job " + id);
        job.setDescription("Description " + id);
        job.setSalary(salary);
        job.setEmployer(employer);
        return job;
    }

    public static Application application(Long id, Job job, Candidate candidate, ApplicationStatus status) {
        Application application = new Application();
        application.setId(id);
        application.setJob(job);
        application.setCandidate(candidate);
        application.setStatus(status);
        application.setCoverLetter("cover");
        return application;
    }
}