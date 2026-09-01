package org.example.veportal.dto.response;

import java.util.List;

public record TestDataStudentFiltersResponse(
        List<String> programmes,
        List<String> batches
) {
}
