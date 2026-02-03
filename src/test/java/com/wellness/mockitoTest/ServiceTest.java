package com.wellness.mockitoTest;

import com.wellness.Repository.IUserRepository;
import com.wellness.Service.UserService;
import com.wellness.data.Role;
import com.wellness.data.Status;
import com.wellness.data.Users;
import com.wellness.dto.UpdateUser;
import com.wellness.dto.UpdateUserAdmin;
import com.wellness.exception.UserAlreadyExistsException;
import com.wellness.exception.UserNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService with mocked IUserRepository and PasswordEncoder.
 */
@ExtendWith(MockitoExtension.class)
class ServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    // ---------------- registerUser ----------------

    @Test
    @DisplayName("registerUser → throws UserAlreadyExistsException when email already exists")
    void registerUser_throwsWhenExists() {
        Users toSave = new Users();
        toSave.setEmail("exists@example.com");
        toSave.setPassword("pw");

        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(toSave));

        verify(userRepository, never()).save(any());
        verify(encoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("registerUser → true when repository.save returns non-null")
    void registerUser_returnsTrueWhenSaved() {
        Users toSave = new Users();
        toSave.setEmail("new@example.com");
        toSave.setPassword("plain");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(encoder.encode("plain")).thenReturn("ENC(plain)");
        when(userRepository.save(any(Users.class))).thenReturn(new Users());

        boolean result = userService.registerUser(toSave);

        assertThat(result).isTrue();
        // Ensure password is encoded before save
        verify(encoder).encode("plain");
        verify(userRepository).save(argThat(u -> "ENC(plain)".equals(u.getPassword())));
    }

    @Test
    @DisplayName("registerUser → false when repository.save returns null")
    void registerUser_returnsFalseWhenSaveReturnsNull() {
        Users toSave = new Users();
        toSave.setEmail("new@example.com");
        toSave.setPassword("pw");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(encoder.encode("pw")).thenReturn("ENC(pw)");
        when(userRepository.save(any(Users.class))).thenReturn(null);

        boolean result = userService.registerUser(toSave);

        assertThat(result).isFalse();
        verify(encoder).encode("pw");
        verify(userRepository).save(any(Users.class));
    }

    // ---------------- getProfile ----------------

    @Test
    @DisplayName("getProfile → returns entity when found")
    void getProfile_found() {
        Users entity = new Users();
        when(userRepository.findById(1)).thenReturn(Optional.of(entity));

        Users result = userService.getProfile(1);

        assertThat(result).isSameAs(entity);
    }

    @Test
    @DisplayName("getProfile → throws UserNotFoundException when not found")
    void getProfile_notFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getProfile(999));
    }

    // ---------------- getUsers ----------------

    @Test
    @DisplayName("getUsers → returns repository.findAll result")
    void getUsers_returnsList() {
        List<Users> list = Arrays.asList(new Users(), new Users(), new Users());
        when(userRepository.findAll()).thenReturn(list);

        List<Users> result = userService.getUsers();

        assertThat(result).hasSize(3).containsExactlyElementsOf(list);
    }

    // ---------------- deleteUser ----------------

    @Test
    @DisplayName("deleteUser → 'User not found!' when id absent")
    void deleteUser_notFound() {
        when(userRepository.findById(111)).thenReturn(Optional.empty());

        String result = userService.deleteUser(111);

        assertThat(result).isEqualTo("User not found!");
        verify(userRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("deleteUser → deletes and returns success message when id present")
    void deleteUser_ok() {
        Users existing = new Users();
        when(userRepository.findById(10)).thenReturn(Optional.of(existing));

        String result = userService.deleteUser(10);

        assertThat(result).isEqualTo("Profile deleted successfully!");
        verify(userRepository).deleteById(10);
    }

    // ---------------- exists ----------------

    @Test
    @DisplayName("exists → delegates to repository.existsByEmail")
    void exists_delegates() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        boolean result = userService.exists("a@b.com");

        assertThat(result).isTrue();
        verify(userRepository).existsByEmail("a@b.com");
    }

    // ---------------- updateUser ----------------

    @Test
    @DisplayName("updateUser → encodes password and saves when email found")
    void updateUser_success() {
        // existing user from DB
        Users existing = new Users();
        existing.setName("Old");
        existing.setDepartment("OldDept");
        existing.setPassword("OLD");

        when(userRepository.findByEmail("emp@example.com")).thenReturn(existing);
        when(encoder.encode("newpw")).thenReturn("ENC(newpw)");
        when(userRepository.save(existing)).thenReturn(existing);

        // request DTO
        UpdateUser req = new UpdateUser();
        req.setEmail("emp@example.com");
        req.setName("New Name");
        req.setDepartment("NewDept");
        req.setPassword("newpw");

        boolean result = userService.updateUser(req);

        assertThat(result).isTrue();
        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getDepartment()).isEqualTo("NewDept");
        assertThat(existing.getPassword()).isEqualTo("ENC(newpw)");
        verify(encoder).encode("newpw");
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("updateUser → throws UserNotFoundException when email not found")
    void updateUser_emailNotFound_throwsUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(null);

        UpdateUser req = new UpdateUser();
        req.setEmail("missing@example.com");
        req.setName("X");
        req.setDepartment("Y");
        req.setPassword("Z");

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(req));
        verify(userRepository, never()).save(any());
        verify(encoder, never()).encode(anyString());
    }

    // ---------------- updateUserAdmin ----------------

    @Test
    @DisplayName("updateUserAdmin → updates fields and saves when email found")
    void updateUserAdmin_success() {
        Users existing = new Users();
        existing.setStatus(Status.INACTIVE);
        existing.setDepartment("OldDept");
        existing.setRole(Role.EMPLOYEE);

        when(userRepository.findByEmail("adminupd@example.com")).thenReturn(existing);
        when(userRepository.save(existing)).thenReturn(existing);

        UpdateUserAdmin req = new UpdateUserAdmin();
        req.setEmail("adminupd@example.com");
        req.setStatus(Status.ACTIVE);
        req.setDepartment("NewDept");
        req.setRole(Role.ADMIN);

        boolean result = userService.updateUserAdmin(req);

        assertThat(result).isTrue();
        assertThat(existing.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(existing.getDepartment()).isEqualTo("NewDept");
        assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("updateUserAdmin → throws UserNotFoundException when email not found")
    void updateUserAdmin_emailNotFound_throwsUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(null);

        UpdateUserAdmin req = new UpdateUserAdmin();
        req.setEmail("missing@example.com");
        req.setStatus(Status.ACTIVE);
        req.setDepartment("Dept");
        req.setRole(Role.ADMIN);

        assertThrows(UserNotFoundException.class, () -> userService.updateUserAdmin(req));
        verify(userRepository, never()).save(any());
    }
}