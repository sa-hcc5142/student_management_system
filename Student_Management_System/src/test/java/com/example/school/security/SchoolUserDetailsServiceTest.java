package com.example.school.security;

import com.example.school.entity.Role;
import com.example.school.entity.User;
import com.example.school.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SchoolUserDetailsService userDetailsService;

    @Test
    @DisplayName("loadUserByUsername returns SchoolUserDetails when user exists")
    void loadUserByUsername_userExists_returnsUserDetails() {
        User user = new User();
        user.setId(1L);
        user.setUsername("teacher1");
        user.setPassword("encoded");
        user.setRole(Role.TEACHER);
        user.setName("Teacher");
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("teacher1");

        assertThat(details).isInstanceOf(SchoolUserDetails.class);
        assertThat(details.getUsername()).isEqualTo("teacher1");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_TEACHER");
        verify(userRepository).findByUsername("teacher1");
    }

    @Test
    @DisplayName("loadUserByUsername returns STUDENT authority for student")
    void loadUserByUsername_student_hasStudentRole() {
        User user = new User();
        user.setId(2L);
        user.setUsername("student1");
        user.setPassword("pwd");
        user.setRole(Role.STUDENT);
        user.setName("Student");
        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("student1");

        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_STUDENT");
    }

    @Test
    @DisplayName("loadUserByUsername throws when user not found")
    void loadUserByUsername_userNotFound_throws() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
