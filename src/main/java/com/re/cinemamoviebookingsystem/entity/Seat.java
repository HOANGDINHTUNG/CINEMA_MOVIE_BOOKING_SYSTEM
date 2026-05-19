package com.re.cinemamoviebookingsystem.entity;

import com.re.cinemamoviebookingsystem.enums.PhysicalSeatType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats", uniqueConstraints = @UniqueConstraint(
        name = "uq_room_seat_position",
        columnNames = {"room_id", "row_name", "seat_number"}
))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "row_name", nullable = false, length = 5)
    private String rowName;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    @Builder.Default
    private PhysicalSeatType seatType = PhysicalSeatType.STANDARD;

    public String getLabel() {
        return rowName + seatNumber;
    }
}
