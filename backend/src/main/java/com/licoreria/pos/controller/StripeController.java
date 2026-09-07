package com.licoreria.pos.controller;

import com.licoreria.pos.dto.StripeConfigDTO;
import com.licoreria.pos.dto.StripePaymentIntentRequestDTO;
import com.licoreria.pos.dto.StripePaymentIntentResponseDTO;
import com.licoreria.pos.service.StripePaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos/stripe")
@RequiredArgsConstructor
@PreAuthorize("@acceso.tiene('VENTAS_CREAR')")
public class StripeController {

    private final StripePaymentService stripePaymentService;

    @GetMapping("/config")
    public ResponseEntity<StripeConfigDTO> config() {
        return ResponseEntity.ok(stripePaymentService.configPublica());
    }

    @PostMapping("/payment-intent")
    public ResponseEntity<StripePaymentIntentResponseDTO> crearIntent(
            @Valid @RequestBody StripePaymentIntentRequestDTO request) {
        return ResponseEntity.ok(stripePaymentService.crearPaymentIntent(request));
    }
}
