package com.kenstudy.miracle_hotel_payment_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kenstudy.miracle_hotel_payment_service.dto.Address;
import com.kenstudy.miracle_hotel_payment_service.dto.BookedRoom;
import com.kenstudy.miracle_hotel_payment_service.model.audit.Audit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Payment implements Audit {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;
    private String guestFullName;
    private String guestEmail;
    @JsonIgnore
    private String stripeNumber;
    @ElementCollection
    @CollectionTable(name = "booked_rooms", joinColumns = @JoinColumn(name = "payment_id"))
    private List<BookedRoom> bookedRooms;
    @Embedded
    private Address address;
    private String status;


    @ElementCollection
    @CollectionTable(name = "customer_sessions", joinColumns = @JoinColumn(name = "payment_id"))
    @MapKeyColumn(name = "customer_id")
    @Column(name = "session_id")
    private Map<String, List<String>> customerSessions;
    private Date date = new Date();

    @Override
    public Date getDate() {
        return date;
    }
}
