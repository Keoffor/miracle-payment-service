package com.kenstudy.miracle_hotel_payment_service.util;

import com.kenstudy.miracle_hotel_payment_service.constant.PaymentConstant;
import com.kenstudy.miracle_hotel_payment_service.dto.BookedRoom;
import com.kenstudy.miracle_hotel_payment_service.dto.PaymentRequest;
import com.kenstudy.miracle_hotel_payment_service.model.Payment;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;

import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

public class CustomerUtils {
    public static Customer findCustomerByEmail(String email) throws StripeException {
        CustomerSearchParams params = CustomerSearchParams.builder()
                .setQuery("email: '" + email + "'")
                .build();
        CustomerSearchResult result = Customer.search(params);
        return result.getData().size() > 0 ? result.getData().get(0) : null;
    }

    public static Customer findOrCreateCustomer(String email, String name) throws StripeException {
        CustomerSearchParams params = CustomerSearchParams.builder()
                .setQuery("email: '" + email + "'")
                .build();
        CustomerSearchResult result = Customer.search(params);
        Customer customer;

        // If no existing customer was found, create a new record
        if(result.getData().size() == 0){
            CustomerCreateParams createCustomer = CustomerCreateParams.builder()
                    .setName(name)
                    .setEmail(email)
                    .build();
            customer = Customer.create(createCustomer);
        }else{
            customer = result.getData().get(0);
        }
        return  customer;
    }

    public static BookedRoom mapBookedRoom(BookedRoom bookedRoomPaymentRequest) {
        BookedRoom bookedRoom = new BookedRoom();
        bookedRoom.setNoOfDays(bookedRoomPaymentRequest.getNoOfDays());
        bookedRoom.setRoomId(bookedRoomPaymentRequest.getRoomId());
        bookedRoom.setRoomType(bookedRoomPaymentRequest.getRoomType());
        bookedRoom.setAmount(bookedRoomPaymentRequest.getAmount());
        bookedRoom.setCurrency(bookedRoomPaymentRequest.getCurrency());
        bookedRoom.setDate(new Date());
        return bookedRoom;
    }

    public static Payment createNewCustomer(PaymentRequest paymentRequest) {
        Payment newCustomer = new Payment();
        newCustomer.setGuestEmail(paymentRequest.getGuestEmail());
        newCustomer.setGuestFullName(paymentRequest.getGuestFullName());
        newCustomer.setStatus(PaymentConstant.INITIATE_PAYMENT.name());
        newCustomer.setAddress(paymentRequest.getAddress());
        newCustomer.setBookedRooms(paymentRequest.getBookedRooms()
                .stream().map(CustomerUtils::mapBookedRoom)
                .collect(Collectors.toCollection(ArrayList::new)));
        newCustomer.setDate(new Date());
        return newCustomer;
    }



}
