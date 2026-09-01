package org.example.veportal.mapper;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import org.example.veportal.dto.response.SessionResponse;
import org.example.veportal.entity.ClassSession;
import org.example.veportal.util.Labels;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    private static final DateTimeFormatter SECONDS_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart()
            .appendFraction(ChronoField.SECOND_OF_MINUTE, 1, 9, true)
            .optionalEnd()
            .toFormatter();

    public SessionResponse toResponse(ClassSession session) {
        return new SessionResponse(
                session.getId().toString(),
                session.getSessionNumber(),
                session.getTopic(),
                session.getSessionDate().toString(),
                session.getStartTime(),
                session.getEndTime(),
                session.getRoom(),
                facultyName(session),
                Labels.of(session.getStatus())
        );
    }

    public String facultyName(ClassSession session) {
        return session.getFaculty() != null ? session.getFaculty().getFullName() : null;
    }

    public String formatTimestamp(java.time.LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(SECONDS_FORMATTER);
    }
}
