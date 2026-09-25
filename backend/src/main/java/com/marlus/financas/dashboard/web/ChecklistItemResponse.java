package com.marlus.financas.dashboard.web;

import java.math.BigDecimal;

public record ChecklistItemResponse(String label, BigDecimal targetAmount, boolean done) {
}
