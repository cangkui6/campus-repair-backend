package com.graduation.repair.integration.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagRetrieveItem {

    private String id;
    private String type;
    private String title;
    private String content;
    private Double score;
    private String source;
}
