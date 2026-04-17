package com.graduation.repair.integration.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagRetrieveResponse {

    private Integer code;
    private String message;
    private DataPayload data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPayload {
        private List<RagRetrieveItem> items = new ArrayList<>();
    }
}
