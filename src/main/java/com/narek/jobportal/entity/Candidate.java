package com.narek.jobportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    private String resumeFilePath;

    private String resumeFileName;

    @Column(length = 500)
    private String parsedResumeSummary;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    public Candidate(Long id, String firstName, String lastName) {
        this.id = id;
        this.fullName = (firstName == null ? "" : firstName)
                + (lastName == null || lastName.isBlank() ? "" : " " + lastName);
        this.resumeFilePath = null;
        this.resumeFileName = null;
        this.parsedResumeSummary = null;
        this.user = null;
    }
}
