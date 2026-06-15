package com.example.dispatchservice.service;

import com.example.dispatchservice.repo.DispatchRepo;
import com.example.dispatchservice.dto.DispatchDTO;
import com.example.dispatchservice.model.Dispatch;
import com.example.dispatchservice.model.DispatchStatus;
import org.springframework.stereotype.Service;

@Service
public class DispatchService {
    private final DispatchRepo dispatchRepo;

    public DispatchService(DispatchRepo dispatchRepo) {
        this.dispatchRepo = dispatchRepo;
    }

    public Dispatch createDispatch(DispatchDTO dispatchDTO) {

        if (dispatchDTO.getPickupLocation().equals("FAILOVER_TEST")) {
            throw new IllegalStateException("FAILOVER TEST: No drivers found!");
        }

        Dispatch createDispatch = new Dispatch();

        createDispatch.setTripId(dispatchDTO.getTripId());
        createDispatch.setCabDriver(dispatchDTO.getCabDriver());
        createDispatch.setCabNo(dispatchDTO.getCabNo());
        createDispatch.setDispatchStatus(DispatchStatus.ASSIGNED);
        createDispatch.setPickupLocation(dispatchDTO.getPickupLocation());

        dispatchRepo.save(createDispatch);
        
        return createDispatch;

    }
}
