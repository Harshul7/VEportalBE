package org.example.veportal.repository;

import java.util.List;
import java.util.Optional;
import org.example.veportal.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByActiveTrueOrderByDisplayOrderAscCodeAsc();
    List<Room> findAllByOrderByDisplayOrderAscCodeAsc();
    Optional<Room> findByCodeIgnoreCase(String code);
}
