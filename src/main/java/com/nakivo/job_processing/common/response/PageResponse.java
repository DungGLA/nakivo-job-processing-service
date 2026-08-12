package com.nakivo.job_processing.common.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {
    private List<T> content;

    private int page;

    private int size;

    private int totalPages;

    private long totalElements;
}
