package com.marlus.financas.dashboard.web;

import java.util.List;

public record DashboardExtrasResponse(
        List<ChecklistItemResponse> payYourselfFirst, List<ScholarshipCountdownResponse> scholarships) {
}
