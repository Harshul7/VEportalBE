package org.example.veportal.dto;

import java.util.List;

public record PagedResult<T>(List<T> content, PageMeta pagination) {
}
