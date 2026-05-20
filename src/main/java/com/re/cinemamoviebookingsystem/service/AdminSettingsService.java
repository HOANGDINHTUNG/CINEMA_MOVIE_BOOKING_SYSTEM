package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.dto.request.CinemaSettingsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSettingsService {

    private final CinemaProperties cinemaProperties;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public CinemaSettingsRequest currentSettings() {
        CinemaSettingsRequest req = new CinemaSettingsRequest();
        req.setSeatLockMinutes(cinemaProperties.getSeatLockMinutes());
        req.setCancelHoursBefore(cinemaProperties.getCancelHoursBefore());
        req.setCleaningBufferMinutes(cinemaProperties.getCleaningBufferMinutes());
        req.setVipPriceMultiplier(cinemaProperties.getVipPriceMultiplier());
        req.setMaxSeatsPerBooking(cinemaProperties.getMaxSeatsPerBooking());
        req.setDemoSeedOnStartup(cinemaProperties.isDemoSeedOnStartup());
        req.setDemoSeedNowShowingTarget(cinemaProperties.getDemoSeedNowShowingTarget());
        req.setDemoSeedComingSoonTarget(cinemaProperties.getDemoSeedComingSoonTarget());
        return req;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(CinemaSettingsRequest request) {
        cinemaProperties.setSeatLockMinutes(request.getSeatLockMinutes());
        cinemaProperties.setCancelHoursBefore(request.getCancelHoursBefore());
        cinemaProperties.setCleaningBufferMinutes(request.getCleaningBufferMinutes());
        cinemaProperties.setVipPriceMultiplier(request.getVipPriceMultiplier());
        cinemaProperties.setMaxSeatsPerBooking(request.getMaxSeatsPerBooking());
        cinemaProperties.setDemoSeedOnStartup(request.isDemoSeedOnStartup());
        cinemaProperties.setDemoSeedNowShowingTarget(request.getDemoSeedNowShowingTarget());
        cinemaProperties.setDemoSeedComingSoonTarget(request.getDemoSeedComingSoonTarget());

        auditLogService.log("SETTINGS_UPDATE", "SETTINGS", null,
                "seatLock=" + request.getSeatLockMinutes()
                        + ", cancelHours=" + request.getCancelHoursBefore()
                        + ", vipMult=" + request.getVipPriceMultiplier());
    }
}
