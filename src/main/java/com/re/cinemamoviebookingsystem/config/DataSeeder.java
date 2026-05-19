package com.re.cinemamoviebookingsystem.config;

import com.re.cinemamoviebookingsystem.entity.*;
import com.re.cinemamoviebookingsystem.enums.PhysicalSeatType;
import com.re.cinemamoviebookingsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Du lieu mac dinh duoc nap tu classpath:db/seed.sql khi khoi dong.
 * Lop nay chi chay neu DB trong (phong khi SQL init bi tat).
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final ComboRepository comboRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) return;
        seed();
    }

    private void seed() {
        Role admin = roleRepository.save(Role.builder().roleName("ADMIN").build());
        Role staff = roleRepository.save(Role.builder().roleName("STAFF").build());
        Role customer = roleRepository.save(Role.builder().roleName("CUSTOMER").build());

        createUser("admin_demo", "admin@smartcinema.com", admin, "Hệ Thống Admin");
        createUser("staff_phuong", "phuong.staff@smartcinema.com", staff, "Nguyễn Thanh Phương");
        createUser("customer_minh", "minh.customer@gmail.com", customer, "Trần Quốc Minh");

        Room r1 = roomRepository.save(Room.builder().roomName("Phòng 1").totalSeats(10).build());
        for (int i = 1; i <= 10; i++) {
            seatRepository.save(Seat.builder().room(r1).rowName("A").seatNumber(i)
                    .seatType(i > 5 ? PhysicalSeatType.VIP : PhysicalSeatType.STANDARD).build());
        }

        comboRepository.save(Combo.builder().name("Combo Solo").price(new BigDecimal("75000")).build());
    }

    private void createUser(String username, String email, Role role, String fullName) {
        User u = User.builder().username(username).email(email).role(role)
                .passwordHash(passwordEncoder.encode("123456")).build();
        u.setProfile(UserProfile.builder().user(u).fullName(fullName).phoneNumber("0900000000").build());
        userRepository.save(u);
    }
}
