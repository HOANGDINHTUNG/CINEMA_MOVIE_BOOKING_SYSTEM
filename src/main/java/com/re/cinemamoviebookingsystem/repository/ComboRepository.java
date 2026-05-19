package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboRepository extends JpaRepository<Combo, Integer> {
    List<Combo> findByStatus(String status);
}
