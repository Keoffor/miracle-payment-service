package com.kenstudy.miracle_hotel_payment_service.dto;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Embeddable
@NoArgsConstructor
public class Address {
    private String street;
    private String state;
    private String zipCode;
}
