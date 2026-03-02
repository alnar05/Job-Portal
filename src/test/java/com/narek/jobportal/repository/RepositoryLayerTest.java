package com.narek.jobportal.repository;

import com.narek.jobportal.entity.*;
import com.narek.jobportal.testsupport.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

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
        userRepository.saveAndFlush(TestEntityFactory.user(null, "candidate1@mail.com", true, Role.CANDIDATE));

        assertThat(userRepository.findByEmail("candidate1@mail.com")).isPresent();
        assertThat(userRepository.findByEmail("missing@mail.com")).isEmpty();
    }

    @Test
    void givenMultipleRoleUsers_whenPaginateByRole_thenPageWorks() {
        for (int i = 0; i < 5; i++) {
            userRepository.save(TestEntityFactory.user(null, "emp" + i + "@mail.com", true, Role.EMPLOYER));
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
        applicationRepository.saveAndFlush(TestEntityFactory.application(null, job, candidate, ApplicationStatus.APPLIED));

        assertThat(applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())).isTrue();
    }

    @Test
    void givenInvalidSalary_whenSaveJob_thenConstraintViolationRaised() {
        Employer employer = persistEmployer("emp2@mail.com");
        Job job = TestEntityFactory.job(null, employer, -1.0);

        assertThatThrownBy(() -> jobRepository.saveAndFlush(job)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenCandidateApplications_whenDeleteCandidateApplications_thenJobRemains() {
        Employer employer = persistEmployer("emp3@mail.com");
        Candidate candidate = persistCandidate("cand3@mail.com", true);
        Job job = persistJob(employer, 3000.0);

        applicationRepository.saveAndFlush(TestEntityFactory.application(null, job, candidate, ApplicationStatus.APPLIED));

        applicationRepository.deleteByCandidateId(candidate.getId());
        applicationRepository.flush();

        assertThat(jobRepository.findById(job.getId())).isPresent();
        assertThat(applicationRepository.findByCandidateId(candidate.getId())).isEmpty();
    }

    @Test
    void givenCandidateWithSameUser_whenPersistSecondCandidate_thenUniqueConstraintViolation() {
        User user = userRepository.save(TestEntityFactory.user(null, "unique@mail.com", true, Role.CANDIDATE));
        candidateRepository.saveAndFlush(TestEntityFactory.candidate(null, user));

        assertThatThrownBy(() -> candidateRepository.saveAndFlush(TestEntityFactory.candidate(null, user)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenEnabledAndDisabledCandidates_whenFilteringInMemory_thenDisabledExcluded() {
        persistCandidate("active@mail.com", true);
        persistCandidate("disabled@mail.com", false);

        List<Candidate> enabledCandidates = candidateRepository.findAll().stream()
                .filter(c -> c.getUser().isEnabled())
                .toList();

        assertThat(enabledCandidates).extracting(c -> c.getUser().getEmail()).containsExactly("active@mail.com");
    }

    private Employer persistEmployer(String email) {
        User user = userRepository.save(TestEntityFactory.user(null, email, true, Role.EMPLOYER));
        Employer employer = TestEntityFactory.employer(null, user);
        return employerRepository.save(employer);
    }

    private Candidate persistCandidate(String email, boolean enabled) {
        User user = userRepository.save(TestEntityFactory.user(null, email, enabled, Role.CANDIDATE));
        Candidate candidate = TestEntityFactory.candidate(null, user);
        return candidateRepository.save(candidate);
    }

    private Job persistJob(Employer employer, double salary) {
        Job job = TestEntityFactory.job(null, employer, salary);
        return jobRepository.save(job);
    }
}