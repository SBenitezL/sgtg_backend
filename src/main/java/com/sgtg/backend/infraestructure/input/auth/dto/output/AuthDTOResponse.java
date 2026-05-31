package com.sgtg.backend.infraestructure.input.auth.dto.output;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class AuthDTOResponse {
    String token;
}
