package com.re.cinemamoviebookingsystem.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ComboSaveRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    @DecimalMin("0")
    private BigDecimal price;

    @NotBlank
    private String status;
}
