package com.re.cinemamoviebookingsystem.util;

import com.re.cinemamoviebookingsystem.dto.response.SeatMapDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SeatLayoutHelper {

    private SeatLayoutHelper() {
    }

    public static Map<String, List<SeatMapDto.SeatCellDto>> groupByRow(List<SeatMapDto.SeatCellDto> seats) {
        Map<String, List<SeatMapDto.SeatCellDto>> rows = new LinkedHashMap<>();
        for (SeatMapDto.SeatCellDto seat : seats) {
            String row = seat.getLabel().replaceAll("[0-9]", "");
            rows.computeIfAbsent(row, k -> new ArrayList<>()).add(seat);
        }
        return rows;
    }
}
