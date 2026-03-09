package com.narek.jobportal.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class AdminDashboardStatsDto {
    long totalJobs;
    long activeJobs;
    long closedJobs;
    long expiredJobs;
    long totalApplications;
    long pendingApplications;
    long acceptedApplications;
    long rejectedApplications;
    long cancelledApplications;
    long totalUsers;
    Map<String, Long> usersByRole;
    Map<String, Long> usersByStatus;
    List<ChartPointDto> jobsOverTime;
    List<ChartPointDto> applicationsOverTime;
    List<ChartPointDto> usersOverTime;
    List<ActivityItemDto> recentJobs;
    List<ActivityItemDto> recentApplications;
    List<ActivityItemDto> recentUsers;
}
