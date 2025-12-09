package com.altoque.altoque.Dto.Payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceResponseDto {
    private String preferenceId;
    private String status;
}