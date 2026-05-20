package com.re.cinemamoviebookingsystem.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CinemaSettingsRequest {

    @Min(1)
    private int seatLockMinutes;

    @Min(0)
    private int cancelHoursBefore;

    @Min(0)
    private int cleaningBufferMinutes;

    @DecimalMin("1.0")
    private double vipPriceMultiplier;

    @Min(1)
    private int maxSeatsPerBooking;

    private boolean demoSeedOnStartup;

    @Min(0)
    private int demoSeedNowShowingTarget;

    @Min(0)
    private int demoSeedComingSoonTarget;
}
