package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountOverviewDto {
    private long totalBookings;
    private long paidCount;
    private long pendingCount;
    private long heldCount;
    private long cancelledCount;
    private long upcomingPaidCount;
}
