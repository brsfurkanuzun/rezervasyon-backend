package com.randevupazaryeri.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private PaginationMeta pagination;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> ok(T data, PaginationMeta pagination) {
        return ApiResponse.<T>builder().success(true).data(data).pagination(pagination).build();
    }

    public static <T> ApiResponse<java.util.List<T>> ofPage(Page<T> page) {
        return ok(page.getContent(), PaginationMeta.from(page));
    }

    public static ApiResponse<Void> empty() {
        return ApiResponse.<Void>builder().success(true).build();
    }
}
