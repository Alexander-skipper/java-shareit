package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.storage.BookingStorage;
import ru.practicum.shareit.exception.EntityNotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingStorage bookingStorage;
    private final ItemService itemService;
    private final UserService userService;

    @Override
    public BookingDto createBooking(BookingDto bookingDto, Long bookerId) {
        checkUserExists(bookerId);

        if (!bookingDto.getEnd().isAfter(bookingDto.getStart())) {
            throw new ValidationException("Дата окончания должна быть позже даты начала");
        }


        ItemDto itemDto = itemService.getItemById(bookingDto.getItemId(), bookerId);
        if (!itemDto.getAvailable()) {
            throw new ValidationException("Вещь недоступна для бронирования");
        }


        List<ItemDto> ownerItems = itemService.getItemsByOwner(bookerId);
        boolean isOwnItem = ownerItems.stream()
                .anyMatch(item -> item.getId().equals(bookingDto.getItemId()));
        if (isOwnItem) {
            throw new EntityNotFoundException("Нельзя бронировать свою собственную вещь");
        }

        Booking booking = BookingMapper.toBooking(bookingDto);
        booking.setBookerId(bookerId);
        booking.setStatus(BookingStatus.WAITING);
        Booking savedBooking = bookingStorage.save(booking);

        return BookingMapper.toBookingDto(savedBooking);
    }

    @Override
    public BookingDto approveBooking(Long bookingId, Long ownerId, boolean approved) {
        Booking booking = bookingStorage.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Бронирование с ID " + bookingId + " не найдено"));


        List<ItemDto> ownerItems = itemService.getItemsByOwner(ownerId);
        boolean isOwner = ownerItems.stream()
                .anyMatch(item -> item.getId().equals(booking.getItemId()));

        if (!isOwner) {
            throw new EntityNotFoundException("Только владелец вещи может подтверждать бронирование");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Бронирование уже обработано");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        Booking updatedBooking = bookingStorage.save(booking);

        return BookingMapper.toBookingDto(updatedBooking);
    }

    @Override
    public BookingDto getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingStorage.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Бронирование с ID " + bookingId + " не найдено"));


        List<ItemDto> ownerItems = itemService.getItemsByOwner(userId);
        boolean isOwner = ownerItems.stream()
                .anyMatch(item -> item.getId().equals(booking.getItemId()));
        boolean isBooker = booking.getBookerId().equals(userId);

        if (!isOwner && !isBooker) {
            throw new EntityNotFoundException("Доступ к бронированию запрещен");
        }

        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public BookingDto cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingStorage.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Бронирование с ID " + bookingId + " не найдено"));

        if (!booking.getBookerId().equals(userId)) {
            throw new EntityNotFoundException("Только автор бронирования может отменить его");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Можно отменить только бронирование в статусе WAITING");
        }

        booking.setStatus(BookingStatus.CANCELED);
        Booking updatedBooking = bookingStorage.save(booking);

        return BookingMapper.toBookingDto(updatedBooking);
    }

    @Override
    public List<BookingDto> getBookingsByBooker(Long bookerId, String state) {
        checkUserExists(bookerId);
        List<Booking> userBookings = bookingStorage.findByBookerId(bookerId).stream()
                .filter(booking -> booking.getBookerId().equals(bookerId))
                .sorted((b1, b2) -> b2.getStart().compareTo(b1.getStart()))
                .collect(Collectors.toList());

        return filterBookingsByState(userBookings, state);
    }

    @Override
    public List<BookingDto> getBookingsByOwner(Long ownerId, String state) {
        checkUserExists(ownerId);


        List<ItemDto> ownerItems = itemService.getItemsByOwner(ownerId);
        List<Long> ownerItemIds = ownerItems.stream()
                .map(ItemDto::getId)
                .collect(Collectors.toList());

        List<Booking> ownerBookings = bookingStorage.findByItemIdIn(ownerItemIds).stream()
                .filter(booking -> ownerItemIds.contains(booking.getItemId()))
                .sorted((b1, b2) -> b2.getStart().compareTo(b1.getStart()))
                .collect(Collectors.toList());

        return filterBookingsByState(ownerBookings, state);
    }

    private List<BookingDto> filterBookingsByState(List<Booking> bookings, String state) {
        if (bookings.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDateTime now = LocalDateTime.now();

        switch (state.toUpperCase()) {
            case "ALL":
                return bookings.stream()
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());

            case "CURRENT":
                return bookings.stream()
                        .filter(booking -> booking.getStart().isBefore(now) && booking.getEnd().isAfter(now))
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());

            case "PAST":
                return bookings.stream()
                        .filter(booking -> booking.getEnd().isBefore(now))
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());

            case "FUTURE":
                return bookings.stream()
                        .filter(booking -> booking.getStart().isAfter(now))
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());

            case "WAITING":
                return bookings.stream()
                        .filter(booking -> booking.getStatus() == BookingStatus.WAITING)
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());

            case "REJECTED":
                return bookings.stream()
                        .filter(booking -> booking.getStatus() == BookingStatus.REJECTED)
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());

            case "CANCELED":
                return bookings.stream()
                        .filter(booking -> booking.getStatus() == BookingStatus.CANCELED)
                        .map(BookingMapper::toBookingDto)
                        .collect(Collectors.toList());

            default:
                throw new ValidationException("Unknown state: " + state);
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

