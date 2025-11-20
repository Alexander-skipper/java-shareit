package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.EntityNotFoundException;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestMapper;
import ru.practicum.shareit.request.storage.ItemRequestStorage;
import ru.practicum.shareit.user.storage.UserStorage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestStorage itemRequestStorage;
    private final UserStorage userStorage;

    @Override
    public ItemRequestDto createRequest(ItemRequestDto itemRequestDto, Long requestorId) {
        checkUserExists(requestorId);

        ItemRequest itemRequest = ItemRequestMapper.toItemRequest(itemRequestDto);
        itemRequest.setRequestorId(requestorId);
        itemRequest.setCreated(LocalDateTime.now());
        ItemRequest savedRequest = itemRequestStorage.save(itemRequest);

        return ItemRequestMapper.toItemRequestDto(savedRequest);
    }

    @Override
    public List<ItemRequestDto> getRequestsByRequestor(Long requestorId) {
        checkUserExists(requestorId);
        return itemRequestStorage.findByRequestorId(requestorId).stream()
                .filter(request -> request.getRequestorId().equals(requestorId))
                .sorted((r1, r2) -> r2.getCreated().compareTo(r1.getCreated()))
                .map(ItemRequestMapper::toItemRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemRequestDto> getAllRequests(Long userId, int from, int size) {
        checkUserExists(userId);

        List<ItemRequest> allRequests = itemRequestStorage.findByRequestorIdNot(userId).stream()
                .filter(request -> !request.getRequestorId().equals(userId))
                .sorted((r1, r2) -> r2.getCreated().compareTo(r1.getCreated()))
                .collect(Collectors.toList());


        int start = Math.min(from, allRequests.size());
        int end = Math.min(from + size, allRequests.size());

        return allRequests.subList(start, end).stream()
                .map(ItemRequestMapper::toItemRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public ItemRequestDto getRequestById(Long requestId, Long userId) {
        checkUserExists(userId);
        ItemRequest itemRequest = itemRequestStorage.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Запрос с ID " + requestId + " не найден"));
        return ItemRequestMapper.toItemRequestDto(itemRequest);
    }

    private void checkUserExists(Long userId) {
        if (!userStorage.existsById(userId)) {
            throw new EntityNotFoundException("Пользователь с ID " + userId + " не найден");
        }
    }
}

