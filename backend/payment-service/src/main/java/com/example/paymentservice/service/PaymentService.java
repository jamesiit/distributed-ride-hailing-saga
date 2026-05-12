package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentDTO;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.repo.PaymentRepo;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepo paymentRepo;

    public PaymentService(PaymentRepo paymentRepo) {
        this.paymentRepo = paymentRepo;
    }

    public Payment createPayment(PaymentDTO paymentDTO) {

        Payment createPayment = new Payment();

        createPayment.setTripId(paymentDTO.getTripId());
        createPayment.setPaymentAmount(paymentDTO.getPaymentAmount());
        createPayment.setPaymentStatus(PaymentStatus.SUCCESS);

        paymentRepo.save(createPayment);
        return createPayment;
    }

    public Payment processRefund(UUID tripId) {

        Payment checkTrip = paymentRepo.findByTripId(tripId);

        if (checkTrip == null) {
            return null;
        }

        checkTrip.setPaymentStatus(PaymentStatus.REFUNDED);
        paymentRepo.save(checkTrip);

        return checkTrip;

    }
}
