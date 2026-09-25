package com.marlus.financas.dashboard.web;

public record AlertResponse(String severity, String message) {

    public static AlertResponse warning(String message) {
        return new AlertResponse("WARNING", message);
    }

    public static AlertResponse danger(String message) {
        return new AlertResponse("DANGER", message);
    }
}
