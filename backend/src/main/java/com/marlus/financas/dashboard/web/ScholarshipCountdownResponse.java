package com.marlus.financas.dashboard.web;

import java.time.YearMonth;
import java.util.UUID;

public record ScholarshipCountdownResponse(UUID sourceId, String name, long remainingMonths, YearMonth endsOn) {
}
