package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.entity.Room;
import com.re.cinemamoviebookingsystem.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LookupService {

    private final RoomRepository roomRepository;

    @Transactional(readOnly = true)
    public List<Room> listRooms() {
        return roomRepository.findAll();
    }
}
