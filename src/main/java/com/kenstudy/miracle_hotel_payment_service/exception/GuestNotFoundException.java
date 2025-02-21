package com.kenstudy.miracle_hotel_payment_service.exception;

public class GuestNotFoundException extends RuntimeException{

    public GuestNotFoundException(String message){
        super(message);
    }
}
