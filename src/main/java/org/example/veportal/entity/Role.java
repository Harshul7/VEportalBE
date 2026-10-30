package org.example.veportal.entity;

public enum Role {
    FACULTY_COORDINATOR,
    GROUP_MENTOR,
    TA,
    /** Legacy values retained so older databases can be read during migration. */
    ADMIN,
    FACULTY;

    public boolean isCoordinator() {
        return this == FACULTY_COORDINATOR || this == ADMIN;
    }

    public boolean isMentor() {
        return this == GROUP_MENTOR || this == FACULTY;
    }

    public boolean canOperateSessions() {
        return isCoordinator() || isMentor() || this == TA;
    }
}
