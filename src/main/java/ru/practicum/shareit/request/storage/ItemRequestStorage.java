package ru.practicum.shareit.request.storage;

import ru.practicum.shareit.request.model.ItemRequest;

import java.util.List;
import java.util.Optional;

public interface ItemRequestStorage {

    ItemRequest save(ItemRequest itemRequest);

    Optional<ItemRequest> findById(Long id);

    List<ItemRequest> findAll();

    void deleteById(Long id);

    List<ItemRequest> findByRequestorId(Long requestorId);

    List<ItemRequest> findByRequestorIdNot(Long requestorId);
}
