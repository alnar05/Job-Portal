package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.repository.EmployerRepository;
import com.narek.jobportal.service.EmployerService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmployerServiceImpl implements EmployerService {

    private final EmployerRepository employerRepository;

    public EmployerServiceImpl(EmployerRepository employerRepository) {
        this.employerRepository = employerRepository;
    }

    @Override
    public Employer saveEmployer(Employer employer) {
        return employerRepository.save(employer);
    }

    @Override
    public Optional<Employer> getEmployerById(Long id) {
        return employerRepository.findById(id);
    }

    @Override
    public Optional<Employer> getEmployerByUserId(Long userId) {
        return employerRepository.findByUserId(userId);
    }
}
