package com.example.dispatchservice.repo;

import com.example.dispatchservice.model.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DispatchRepo extends JpaRepository<Dispatch, UUID> {
}
