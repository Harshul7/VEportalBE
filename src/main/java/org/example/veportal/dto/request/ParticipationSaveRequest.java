package org.example.veportal.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ParticipationSaveRequest(
        @Valid
        @NotEmpty(message = "At least one participation entry is required")
        List<ParticipationEntryRequest> entries
) {
}
