package com.kntransport.backend.repository;

import com.kntransport.backend.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    boolean existsByPlate(String plate);
    Optional<Vehicle> findByPlate(String plate);
    Page<Vehicle> findAllByOrderByMakeAscModelAsc(Pageable pageable);
    Page<Vehicle> findByActiveOrderByMakeAscModelAsc(boolean active, Pageable pageable);
}
