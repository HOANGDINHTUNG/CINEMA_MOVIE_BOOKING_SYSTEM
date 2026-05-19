package com.re.cinemamoviebookingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class PtitCntt1It210ProjectfinalCinemaMovieBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(PtitCntt1It210ProjectfinalCinemaMovieBookingSystemApplication.class, args);
    }

}
