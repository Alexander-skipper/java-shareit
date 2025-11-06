package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.EntityNotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestMapper;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemRequestServiceImpl implements ItemRequestService {
    private final Map<Long, ItemRequest> requests = new HashMap<>();
    private long idCounter = 1;
    private final UserService userService;

    @Override
    public ItemRequestDto createRequest(ItemRequestDto itemRequestDto, Long requestorId) {
        validateRequest(itemRequestDto);
        checkUserExists(requestorId);

        ItemRequest itemRequest = ItemRequestMapper.toItemRequest(itemRequestDto);
        itemRequest.setId(idCounter++);
        itemRequest.setRequestorId(requestorId);
        itemRequest.setCreated(LocalDateTime.now());
        requests.put(itemRequest.getId(), itemRequest);

        return ItemRequestMapper.toItemRequestDto(itemRequest);
    }

    @Override
    public List<ItemRequestDto> getRequestsByRequestor(Long requestorId) {
        checkUserExists(requestorId);
        return requests.values().stream()
                .filter(request -> request.getRequestorId().equals(requestorId))
                .sorted((r1, r2) -> r2.getCreated().compareTo(r1.getCreated()))
                .map(ItemRequestMapper::toItemRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemRequestDto> getAllRequests(Long userId, int from, int size) {
        checkUserExists(userId);

        if (from < 0 || size <= 0) {
            throw new ValidationException("Неверные параметры пагинации");
        }

        List<ItemRequest> allRequests = requests.values().stream()
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
        ItemRequest itemRequest = requests.get(requestId);
        if (itemRequest == null) {
            throw new EntityNotFoundException("Запрос с ID " + requestId + " не найден");
        }
        return ItemRequestMapper.toItemRequestDto(itemRequest);
    }

    private void validateRequest(ItemRequestDto itemRequestDto) {
        if (itemRequestDto.getDescription() == null || itemRequestDto.getDescription().isBlank()) {
            throw new ValidationException("Описание запроса не может быть пустым");
        }
    }

    private void checkUserExists(Long userId) {
        try {
            userService.getUserById(userId);
        } catch (EntityNotFoundException e) {
            throw new EntityNotFoundException("Пользователь с ID " + userId + " не найден");
        }
    }
}

