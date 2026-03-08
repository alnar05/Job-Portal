package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void saveUser_shouldPersistAndReturnSavedUser() {
        User input = new User();
        input.setEmail("user@example.com");

        User saved = new User();
        saved.setId(1L);
        saved.setEmail("user@example.com");

        given(userRepository.save(input)).willReturn(saved);

        User result = userService.saveUser(input);

        assertEquals(1L, result.getId());
        assertEquals("user@example.com", result.getEmail());
        verify(userRepository).save(input);
    }

    @Test
    void getUserById_shouldReturnUser_whenPresent() {
        User user = new User();
        user.setId(7L);
        user.setEmail("id@example.com");
        given(userRepository.findById(7L)).willReturn(Optional.of(user));

        Optional<User> result = userService.getUserById(7L);

        assertTrue(result.isPresent());
        assertEquals("id@example.com", result.get().getEmail());
        verify(userRepository).findById(7L);
    }

    @Test
    void getUserById_shouldReturnEmpty_whenMissing() {
        given(userRepository.findById(7L)).willReturn(Optional.empty());

        Optional<User> result = userService.getUserById(7L);

        assertTrue(result.isEmpty());
        verify(userRepository).findById(7L);
    }

    @Test
    void getUserByEmail_shouldReturnUser_whenPresent() {
        User user = new User();
        user.setEmail("mail@example.com");
        given(userRepository.findByEmail("mail@example.com")).willReturn(Optional.of(user));

        Optional<User> result = userService.getUserByEmail("mail@example.com");

        assertTrue(result.isPresent());
        assertEquals("mail@example.com", result.get().getEmail());
        verify(userRepository).findByEmail("mail@example.com");
    }

    @Test
    void getUserByEmail_shouldReturnEmpty_whenMissing() {
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        Optional<User> result = userService.getUserByEmail("missing@example.com");

        assertTrue(result.isEmpty());
        verify(userRepository).findByEmail("missing@example.com");
    }

    @Test
    void deleteUser_shouldDeleteUser_whenUserExists() {
        User user = new User();
        user.setId(3L);

        given(userRepository.findById(3L)).willReturn(Optional.of(user));

        userService.deleteUser(3L);

        verify(userRepository).findById(3L);
        verify(userRepository).delete(user);
    }
}