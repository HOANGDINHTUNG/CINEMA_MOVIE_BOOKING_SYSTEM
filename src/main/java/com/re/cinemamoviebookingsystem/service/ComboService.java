package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.request.ComboSaveRequest;
import com.re.cinemamoviebookingsystem.entity.Combo;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.ComboRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ComboService {

    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");

    private final ComboRepository comboRepository;

    @Transactional(readOnly = true)
    public List<Combo> listAll() {
        return comboRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Combo get(Integer id) {
        return comboRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Combo không tồn tại"));
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer create(ComboSaveRequest request) {
        validateStatus(request.getStatus());
        Combo combo = Combo.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .status(request.getStatus().trim().toUpperCase())
                .build();
        return comboRepository.save(combo).getComboId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Integer id, ComboSaveRequest request) {
        validateStatus(request.getStatus());
        Combo combo = get(id);
        combo.setName(request.getName().trim());
        combo.setDescription(request.getDescription());
        combo.setPrice(request.getPrice());
        combo.setStatus(request.getStatus().trim().toUpperCase());
        comboRepository.save(combo);
    }

    private void validateStatus(String status) {
        if (status == null || !STATUSES.contains(status.trim().toUpperCase())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Trạng thái phải là ACTIVE hoặc INACTIVE");
        }
    }
}
