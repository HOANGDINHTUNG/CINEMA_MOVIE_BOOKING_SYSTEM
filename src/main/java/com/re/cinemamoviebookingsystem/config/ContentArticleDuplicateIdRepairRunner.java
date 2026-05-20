package com.re.cinemamoviebookingsystem.config;

import com.re.cinemamoviebookingsystem.service.ContentArticleRepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(12)
@RequiredArgsConstructor
public class ContentArticleDuplicateIdRepairRunner implements ApplicationRunner {

    private final ContentArticleRepairService contentArticleRepairService;

    @Override
    public void run(ApplicationArguments args) {
        contentArticleRepairService.repairMissingNewsAndFestival();
    }
}
