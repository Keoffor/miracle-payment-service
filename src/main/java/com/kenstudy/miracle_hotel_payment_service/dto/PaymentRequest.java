package com.kenstudy.miracle_hotel_payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String guestFullName;
    private String guestEmail;
    private List<BookedRoom> bookedRooms;
    private Address address;

}
