package com.kenstudy.miracle_hotel_payment_service.exception.advice;

import com.kenstudy.miracle_hotel_payment_service.exception.ErrorMessage;
import com.kenstudy.miracle_hotel_payment_service.exception.GuestNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(GuestNotFoundException.class)
    public ResponseEntity<?>guestNotFoundExceptionHandler(GuestNotFoundException ex){
        ErrorMessage errorMessage = ErrorMessage.builder()
                .errorMessage(ex.getMessage())
                .status(HttpStatus.NOT_FOUND)
                .errorCode("PAYMENT-SERVICE-ERROR")
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
    }

}
