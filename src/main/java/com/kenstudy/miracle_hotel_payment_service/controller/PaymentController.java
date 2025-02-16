package com.kenstudy.miracle_hotel_payment_service.controller;

import com.kenstudy.miracle_hotel_payment_service.dto.PaymentBookedRoomDTO;
import com.kenstudy.miracle_hotel_payment_service.dto.PaymentRequest;
import com.kenstudy.miracle_hotel_payment_service.dto.response.PaymentResponse;
import com.kenstudy.miracle_hotel_payment_service.model.Payment;
import com.kenstudy.miracle_hotel_payment_service.service.custom_impl.PaymentServiceImpl;
import com.stripe.exception.StripeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payment")
@CrossOrigin("*")
public class PaymentController {

    private PaymentServiceImpl paymentServiceImpl;

    public PaymentController(PaymentServiceImpl paymentServiceImpl) {
        this.paymentServiceImpl = paymentServiceImpl;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> paymentCheckout(@RequestBody PaymentRequest paymentRequest) throws StripeException {
        try {
            PaymentResponse paymentResponse = paymentServiceImpl.bookedRoomCheckout(paymentRequest);
            return ResponseEntity.status(HttpStatus.OK).body(paymentResponse);
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/customer-bookings")
    public ResponseEntity<?> findCustomerBookedRoomsByGuestEmailAndCustomerId(
            @RequestParam String email,
            @RequestParam String customerId) {

        List<PaymentBookedRoomDTO> customerBookingRooms = paymentServiceImpl
                .findBookedRoomsByEmailAndCustomerId(email, customerId);

        if (customerBookingRooms.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer has no booking record");
        }
        return ResponseEntity.ok(customerBookingRooms);
    }
}
