package com.codecore.appointment.application.query;

/**
 * List status filter for Appointment administration (PASO 18.5.1).
 */
public enum AppointmentListFilter {
    SCHEDULED,
    CANCELLED,
    COMPLETED,
    ALL;

    public static AppointmentListFilter parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return SCHEDULED;
        }
        return AppointmentListFilter.valueOf(raw.trim().toUpperCase());
    }
}
