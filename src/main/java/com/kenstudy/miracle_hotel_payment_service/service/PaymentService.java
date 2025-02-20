package com.kenstudy.miracle_hotel_payment_service.service;

import com.kenstudy.miracle_hotel_payment_service.dto.PaymentBookedRoomDTO;
import com.kenstudy.miracle_hotel_payment_service.dto.PaymentRequest;
import com.kenstudy.miracle_hotel_payment_service.dto.response.PaymentResponse;
import com.kenstudy.miracle_hotel_payment_service.model.Payment;
import com.stripe.exception.StripeException;

import java.util.List;

public interface PaymentService {
    PaymentResponse bookedRoomCheckout(PaymentRequest paymentRequest) throws StripeException;

    List<PaymentBookedRoomDTO> findBookedRoomsByEmailAndCustomerId(String email, String customer);
}
