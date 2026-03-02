package com.narek.jobportal.security;

import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.UserRepository;
import com.narek.jobportal.testsupport.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void givenSoftDeletedUser_whenLoadUserByUsername_thenUserIsDisabledInUserDetails() {
        User disabledUser = TestEntityFactory.user(1L, "disabled@mail.com", false, Role.CANDIDATE);
        when(userRepository.findByEmail("disabled@mail.com")).thenReturn(Optional.of(disabledUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("disabled@mail.com");

        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.getUsername()).isEqualTo("disabled@mail.com");
    }
}