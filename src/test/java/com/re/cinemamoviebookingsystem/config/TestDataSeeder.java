package com.re.cinemamoviebookingsystem.config;

import com.re.cinemamoviebookingsystem.entity.*;
import com.re.cinemamoviebookingsystem.enums.MovieStatus;
import com.re.cinemamoviebookingsystem.enums.PhysicalSeatType;
import com.re.cinemamoviebookingsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Profile("test")
@RequiredArgsConstructor
public class TestDataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) return;
        Role customer = roleRepository.save(Role.builder().roleName("CUSTOMER").build());
        roleRepository.save(Role.builder().roleName("ADMIN").build());
        roleRepository.save(Role.builder().roleName("STAFF").build());
        User u = User.builder().username("test").email("t@t.com").role(customer)
                .passwordHash(passwordEncoder.encode("123456")).build();
        u.setProfile(UserProfile.builder().user(u).fullName("Test").build());
        userRepository.save(u);
        Room r = roomRepository.save(Room.builder().roomName("R1").totalSeats(5).build());
        for (int i = 1; i <= 5; i++) {
            seatRepository.save(Seat.builder().room(r).rowName("A").seatNumber(i)
                    .seatType(PhysicalSeatType.STANDARD).build());
        }
        movieRepository.save(Movie.builder().title("T").duration(90).releaseDate(LocalDate.now())
                .status(MovieStatus.ACTIVE).build());
    }
}
