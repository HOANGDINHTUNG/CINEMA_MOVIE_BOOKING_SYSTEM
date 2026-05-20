package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.request.RoomCreateRequest;
import com.re.cinemamoviebookingsystem.entity.Room;
import com.re.cinemamoviebookingsystem.entity.Seat;
import com.re.cinemamoviebookingsystem.enums.PhysicalSeatType;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.RoomRepository;
import com.re.cinemamoviebookingsystem.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    @Transactional(readOnly = true)
    public List<Room> listAll() {
        return roomRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Room get(Integer roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Phòng không tồn tại"));
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer create(RoomCreateRequest request) {
        if (roomRepository.existsByRoomName(request.getRoomName().trim())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Tên phòng đã tồn tại");
        }
        int total = request.getRows() * request.getSeatsPerRow();
        Room room = Room.builder()
                .roomName(request.getRoomName().trim())
                .totalSeats(total)
                .build();
        room = roomRepository.save(room);
        generateSeats(room, request.getRows(), request.getSeatsPerRow(),
                request.getVipRowsFromEnd() != null ? request.getVipRowsFromEnd() : 0);
        return room.getRoomId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateName(Integer roomId, String roomName) {
        Room room = get(roomId);
        String name = roomName.trim();
        if (!name.equals(room.getRoomName()) && roomRepository.existsByRoomName(name)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Tên phòng đã tồn tại");
        }
        room.setRoomName(name);
        roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public List<Seat> listSeats(Integer roomId) {
        return seatRepository.findByRoomRoomId(roomId);
    }

    private void generateSeats(Room room, int rows, int seatsPerRow, int vipRowsFromEnd) {
        List<Seat> seats = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            char rowName = (char) ('A' + r);
            boolean vip = vipRowsFromEnd > 0 && r >= rows - vipRowsFromEnd;
            for (int num = 1; num <= seatsPerRow; num++) {
                seats.add(Seat.builder()
                        .room(room)
                        .rowName(String.valueOf(rowName))
                        .seatNumber(num)
                        .seatType(vip ? PhysicalSeatType.VIP : PhysicalSeatType.STANDARD)
                        .build());
            }
        }
        seatRepository.saveAll(seats);
    }
}
