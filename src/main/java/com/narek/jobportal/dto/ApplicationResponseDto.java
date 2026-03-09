package com.narek.jobportal.dto;

import com.narek.jobportal.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponseDto {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long candidateId;
    private String candidateName;
    private String coverLetter;
    private String internalNotes;
    private LocalDateTime appliedAt;
    private ApplicationStatus status;

    public ApplicationResponseDto(Long id,
                                  Long jobId,
                                  String jobTitle,
                                  Long candidateId,
                                  String candidateName,
                                  String coverLetter,
                                  LocalDateTime appliedAt,
                                  ApplicationStatus status) {
        this.id = id;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.coverLetter = coverLetter;
        this.internalNotes = null;
        this.appliedAt = appliedAt;
        this.status = status;
    }
}
