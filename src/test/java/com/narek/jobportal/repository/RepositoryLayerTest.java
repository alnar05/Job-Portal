package com.narek.jobportal.repository;

import com.narek.jobportal.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class RepositoryLayerTest {

    @Autowired private UserRepository userRepository;
    @Autowired private EmployerRepository employerRepository;
    @Autowired private CandidateRepository candidateRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private ApplicationRepository applicationRepository;

    @Test
    void givenUsers_whenFindByEmail_thenReturnsCorrectUser() {
        User user = new User();
        user.setEmail("candidate1@mail.com");
        user.setPassword("encoded");
        user.setEnabled(true);
        user.setRoles(Set.of(Role.CANDIDATE));
        userRepository.saveAndFlush(user);

        assertThat(userRepository.findByEmail("candidate1@mail.com")).isPresent();
        assertThat(userRepository.findByEmail("missing@mail.com")).isEmpty();
    }

    @Test
    void givenMultipleRoleUsers_whenPaginateByRole_thenPageWorks() {
        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setEmail("emp" + i + "@mail.com");
            user.setPassword("encoded");
            user.setEnabled(true);
            user.setRoles(Set.of(Role.EMPLOYER));
            userRepository.save(user);
        }

        Page<User> page = userRepository.findAllByRole(Role.EMPLOYER, PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    @Test
    void givenApplicationForCandidateAndJob_whenExistsQuery_thenDuplicateDetected() {
        Employer employer = persistEmployer("emp@mail.com");
        Candidate candidate = persistCandidate("cand@mail.com", true);
        Job job = persistJob(employer, 2000.0);

        Application application = new Application();
        application.setJob(job);
        application.setCandidate(candidate);
        application.setCoverLetter("first");
        applicationRepository.saveAndFlush(application);

        assertThat(applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())).isTrue();
    }

    @Test
    void givenInvalidSalary_whenSaveJob_thenConstraintViolationRaised() {
        Employer employer = persistEmployer("emp2@mail.com");
        Job job = new Job();
        job.setTitle("Invalid");
        job.setDescription("invalid");
        job.setSalary(-1.0);
        job.setEmployer(employer);

        assertThatThrownBy(() -> jobRepository.saveAndFlush(job))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenCandidateApplications_whenDeleteCandidateApplications_thenJobRemains() {
        Employer employer = persistEmployer("emp3@mail.com");
        Candidate candidate = persistCandidate("cand3@mail.com", true);
        Job job = persistJob(employer, 3000.0);

        Application application = new Application();
        application.setJob(job);
        application.setCandidate(candidate);
        applicationRepository.saveAndFlush(application);

        applicationRepository.deleteByCandidateId(candidate.getId());
        applicationRepository.flush();

        assertThat(jobRepository.findById(job.getId())).isPresent();
        assertThat(applicationRepository.findByCandidateId(candidate.getId())).isEmpty();
    }

    @Test
    void givenEnabledAndDisabledUsers_whenFilteringEnabled_thenDisabledExcluded() {
        persistCandidate("active@mail.com", true);
        persistCandidate("disabled@mail.com", false);

        var enabledCandidates = candidateRepository.findAll().stream()
                .filter(c -> c.getUser().isEnabled())
                .toList();

        assertThat(enabledCandidates).extracting(c -> c.getUser().getEmail())
                .containsExactly("active@mail.com");
    }

    private Employer persistEmployer(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("encoded");
        user.setEnabled(true);
        user.setRoles(Set.of(Role.EMPLOYER));
        user = userRepository.save(user);

        Employer employer = new Employer();
        employer.setCompanyName("ACME");
        employer.setWebsite("https://acme.example");
        employer.setUser(user);
        return employerRepository.save(employer);
    }

    private Candidate persistCandidate(String email, boolean enabled) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("encoded");
        user.setEnabled(enabled);
        user.setRoles(Set.of(Role.CANDIDATE));
        user = userRepository.save(user);

        Candidate candidate = new Candidate();
        candidate.setFullName("Candidate");
        candidate.setResumeUrl("https://resume.example");
        candidate.setUser(user);
        return candidateRepository.save(candidate);
    }

    private Job persistJob(Employer employer, double salary) {
        Job job = new Job();
        job.setTitle("Java Dev");
        job.setDescription("desc");
        job.setSalary(salary);
        job.setEmployer(employer);
        return jobRepository.save(job);
    }
}