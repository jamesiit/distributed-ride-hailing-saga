package com.example.dispatchservice.controller;

import com.example.dispatchservice.dto.DispatchDTO;
import com.example.dispatchservice.model.Dispatch;
import com.example.dispatchservice.service.DispatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @GetMapping("/dispatch")
    public String sayHello() {
        return "Hi";
    }

    @PostMapping("/dispatch")
    public ResponseEntity<?> createDispatch(@RequestBody DispatchDTO dispatchDTO) {
        try {

            Dispatch createDispatch = dispatchService.createDispatch(dispatchDTO);
            return new ResponseEntity<>(createDispatch.getDispatchId(), HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<>("Something went wrong!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
