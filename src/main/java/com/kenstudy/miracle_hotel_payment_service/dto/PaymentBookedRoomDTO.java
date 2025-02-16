package com.kenstudy.miracle_hotel_payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentBookedRoomDTO {
    private Long paymentId;
    private String guestFullName;
    private String guestEmail;
    private String status;
    private Long roomId;
    private String roomType;
    private BigDecimal amount;
    private Date date;


    // Getters and Setters
}

