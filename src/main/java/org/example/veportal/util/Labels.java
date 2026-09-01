package org.example.veportal.util;

import org.example.veportal.entity.AccountStatus;
import org.example.veportal.entity.ActivityKind;
import org.example.veportal.entity.AttendanceStatus;
import org.example.veportal.entity.MaterialType;
import org.example.veportal.entity.ParticipationLevel;
import org.example.veportal.entity.Role;
import org.example.veportal.entity.SessionStatus;
import org.example.veportal.exception.BusinessException;

public final class Labels {

    private Labels() {
    }

    public static String of(SessionStatus status) {
        return switch (status) {
            case UPCOMING -> "Upcoming";
            case COMPLETED -> "Completed";
            case DRAFT -> "Draft";
        };
    }

    public static SessionStatus toSessionStatus(String label) {
        return switch (label) {
            case "Upcoming" -> SessionStatus.UPCOMING;
            case "Completed" -> SessionStatus.COMPLETED;
            case "Draft" -> SessionStatus.DRAFT;
            default -> throw new BusinessException("Unknown session status: " + label);
        };
    }

    public static String of(AccountStatus status) {
        return switch (status) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
        };
    }

    public static AccountStatus toAccountStatus(String label) {
        return switch (label) {
            case "Active" -> AccountStatus.ACTIVE;
            case "Inactive" -> AccountStatus.INACTIVE;
            default -> throw new BusinessException("Unknown status: " + label);
        };
    }

    public static String of(AttendanceStatus status) {
        return switch (status) {
            case PRESENT -> "Present";
            case ABSENT -> "Absent";
        };
    }

    public static AttendanceStatus toAttendanceStatus(String label) {
        return switch (label) {
            case "Present" -> AttendanceStatus.PRESENT;
            case "Absent" -> AttendanceStatus.ABSENT;
            default -> throw new BusinessException("Unknown attendance status: " + label);
        };
    }

    public static String of(ParticipationLevel level) {
        return switch (level) {
            case NOT_RECORDED -> "Not Recorded";
            case LOW -> "Low";
            case MODERATE -> "Moderate";
            case HIGH -> "High";
        };
    }

    public static ParticipationLevel toParticipationLevel(String label) {
        return switch (label) {
            case "Not Recorded" -> ParticipationLevel.NOT_RECORDED;
            case "Low" -> ParticipationLevel.LOW;
            case "Moderate" -> ParticipationLevel.MODERATE;
            case "High" -> ParticipationLevel.HIGH;
            default -> throw new BusinessException("Unknown participation level: " + label);
        };
    }

    public static String of(MaterialType type) {
        return switch (type) {
            case LECTURE_NOTES -> "Lecture Notes";
            case SLIDES -> "Slides";
            case ASSIGNMENT_BRIEF -> "Assignment Brief";
            case READING_MATERIAL -> "Reading Material";
            case REFERENCE_PDF -> "Reference PDF";
        };
    }

    public static MaterialType toMaterialType(String label) {
        return switch (label) {
            case "Lecture Notes" -> MaterialType.LECTURE_NOTES;
            case "Slides" -> MaterialType.SLIDES;
            case "Assignment Brief" -> MaterialType.ASSIGNMENT_BRIEF;
            case "Reading Material" -> MaterialType.READING_MATERIAL;
            case "Reference PDF" -> MaterialType.REFERENCE_PDF;
            default -> throw new BusinessException("Unknown material type: " + label);
        };
    }

    public static String of(Role role) {
        return switch (role) {
            case FACULTY -> "Faculty / Mentor";
            case ADMIN -> "Administrator";
        };
    }

    public static String of(ActivityKind kind) {
        return switch (kind) {
            case ATTENDANCE -> "attendance";
            case LOG -> "log";
            case MATERIAL -> "material";
            case PARTICIPATION -> "participation";
        };
    }
}
