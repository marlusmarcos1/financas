package com.marlus.financas.calendar.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CalendarItemResponse(LocalDate date, String type, String description, BigDecimal amount) {
}
