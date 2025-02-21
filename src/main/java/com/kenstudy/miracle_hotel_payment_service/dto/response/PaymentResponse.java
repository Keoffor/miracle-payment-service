package com.kenstudy.miracle_hotel_payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PaymentResponse {
    private String customerId;
    private String status;
    private String message;
    private String sessionId;
    private String stripeUrl;
}
