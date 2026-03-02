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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void givenMissingUser_whenLoadUserByUsername_thenThrow() {
        when(userRepository.findByEmail("missing@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing@mail.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void givenDisabledUser_whenLoadUserByUsername_thenUserDetailsDisabled() {
        User disabledUser = TestEntityFactory.user(1L, "disabled@mail.com", false, Role.CANDIDATE);
        when(userRepository.findByEmail("disabled@mail.com")).thenReturn(Optional.of(disabledUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("disabled@mail.com");

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void givenEnabledUser_whenLoadUserByUsername_thenMapsAuthorities() {
        User user = TestEntityFactory.user(1L, "employer@mail.com", true, Role.EMPLOYER);
        when(userRepository.findByEmail("employer@mail.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("employer@mail.com");

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_EMPLOYER");
    }
}