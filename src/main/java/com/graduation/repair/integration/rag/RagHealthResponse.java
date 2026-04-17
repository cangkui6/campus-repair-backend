package com.graduation.repair.integration.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagHealthResponse {

    private Integer code;
    private String message;
    private DataPayload data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPayload {
        private String status;
        private String model;
        private Integer kbSize;
    }
}
