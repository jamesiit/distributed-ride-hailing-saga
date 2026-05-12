package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentDTO;
import com.example.paymentservice.dto.RefundDTO;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/test/payment")
    public String sayHello() {
        return "Hi";
    }

    @PostMapping("/payment")
    public ResponseEntity<?> processPayment(@RequestBody PaymentDTO paymentDTO) {
        try {
            Payment createPayment = paymentService.createPayment(paymentDTO);

            return new ResponseEntity<>(createPayment.getPaymentId(), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/payments/refund")
    public ResponseEntity<?> refundPayment(@RequestBody RefundDTO refundDTO) {
        Payment refundPayment = paymentService.processRefund(refundDTO.getTripId());

        if (refundPayment == null) {
            return new ResponseEntity<>("Trip not found", HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(refundPayment, HttpStatus.OK);

    }

}
