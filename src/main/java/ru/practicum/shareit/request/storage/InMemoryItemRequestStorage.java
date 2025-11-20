package ru.practicum.shareit.request.storage;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.request.model.ItemRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InMemoryItemRequestStorage implements ItemRequestStorage {
    private final Map<Long, ItemRequest> requests = new HashMap<>();
    private long idCounter = 1;

    @Override
    public ItemRequest save(ItemRequest itemRequest) {
        if (itemRequest.getId() == null) {
            itemRequest.setId(idCounter++);
        }
        requests.put(itemRequest.getId(), itemRequest);
        return itemRequest;
    }

    @Override
    public Optional<ItemRequest> findById(Long id) {
        return Optional.ofNullable(requests.get(id));
    }

    @Override
    public List<ItemRequest> findAll() {
        return new ArrayList<>(requests.values());
    }

    @Override
    public void deleteById(Long id) {
        requests.remove(id);
    }

    @Override
    public List<ItemRequest> findByRequestorId(Long requestorId) {
        return requests.values().stream()
                .filter(request -> request.getRequestorId().equals(requestorId))
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemRequest> findByRequestorIdNot(Long requestorId) {
        return requests.values().stream()
                .filter(request -> !request.getRequestorId().equals(requestorId))
                .collect(Collectors.toList());
    }
}
