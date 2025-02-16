package com.kenstudy.miracle_hotel_payment_service.dto;

import com.kenstudy.miracle_hotel_payment_service.model.audit.Audit;
import com.stripe.model.Product;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class BookedRoom extends Product implements Audit {
    private Long roomId;
    private String roomType;
    private BigDecimal amount;
    private Long noOfDays;
    private String currency;
    private Date date;

    @Override
    public Date getDate() {
        return date;
    }
}
