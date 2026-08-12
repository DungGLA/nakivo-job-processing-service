package com.nakivo.job_processing.common.helper;

import com.nakivo.job_processing.common.response.PageResponse;
import org.springframework.data.domain.Page;

public class PageResponseMapper {
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }
}
