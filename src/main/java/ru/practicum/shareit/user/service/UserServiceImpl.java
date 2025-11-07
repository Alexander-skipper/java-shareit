package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.DuplicateEmailException;
import ru.practicum.shareit.exception.EntityNotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserStorage;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserStorage userStorage;


    @Override
    public UserDto createUser(UserDto userDto) {
        checkEmailUniqueness(userDto.getEmail());

        User user = UserMapper.toUser(userDto);
        User savedUser = userStorage.save(user);

        return UserMapper.toUserDto(savedUser);
    }

    @Override
    public UserDto updateUser(Long userId, UserDto userDto) {
        User existingUser = userStorage.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + userId + " не найден"));

        if (userDto.getName() != null) {
            existingUser.setName(userDto.getName());
        }

        if (userDto.getEmail() != null && !userDto.getEmail().equals(existingUser.getEmail())) {
            checkEmailUniqueness(userDto.getEmail());
            existingUser.setEmail(userDto.getEmail());
        }

        User updatedUser = userStorage.save(existingUser);
        return UserMapper.toUserDto(updatedUser);
    }

    @Override
    public UserDto getUserById(Long userId) {
        User user = userStorage.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + userId + " не найден"));
        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {

        return userStorage.findAll().stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long userId) {
        if (!userStorage.findById(userId).isPresent()) {
            throw new EntityNotFoundException("Пользователь с ID " + userId + " не найден");
        }
        userStorage.deleteById(userId);
    }

    private void checkEmailUniqueness(String email) {
        if (userStorage.existsByEmail(email)) {
            throw new DuplicateEmailException("Пользователь с email " + email + " уже существует");
        }
    }
}
