package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ShowtimeBulkCreateResultDto {
    private int created;
    private int skippedConflicts;
    private List<String> conflictMessages;
}
