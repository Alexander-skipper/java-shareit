package ru.practicum.shareit.booking.storage;

import ru.practicum.shareit.booking.model.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingStorage {
    Booking save(Booking booking);
    Optional<Booking> findById(Long id);
    List<Booking> findAll();
    void deleteById(Long id);
    List<Booking> findByBookerId(Long bookerId);
    List<Booking> findByItemIdIn(List<Long> itemIds);
}
