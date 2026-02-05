package com.wellness.mockitoTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.wellness.controller.MyController;
import com.wellness.data.Users;
import com.wellness.service.JwtService;
import com.wellness.service.MyUserDetailsService;
import com.wellness.service.UserService;

/**
 * Controller tests for MyController (updated to new exception-based responses).
 * - Uses @WebMvcTest + @MockitoBean
 * - Adds a test-only SecurityFilterChain to prevent formLogin() from hijacking /login
 * - Uses @WithMockUser for @PreAuthorize methods and .with(csrf()) for mutating requests
 */
@WebMvcTest(MyController.class)
@Import(ControllerTest.SecurityTestConfig.class)
public class ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ---- Mock collaborators used by MyController ----
    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private MyUserDetailsService myUserDetailsService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    // ---------- Test-only Security config to let the controller handle /login ----------
    @TestConfiguration
    static class SecurityTestConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http){
            http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
            return http.build();
        }
    }

    // ------------- REGISTER -----------------
    @Nested
    class RegisterTests {

        @Test
        @WithMockUser
        @DisplayName("POST /register → 200 when user not exists and registration succeeds")
        void register_success() {
            try {
                String email = "user@example.com";
                String json = "{\"email\":\"" + email + "\",\"password\":\"pw\"}";

                when(userService.exists(email)).thenReturn(false);
                when(userService.registerUser(org.mockito.ArgumentMatchers.any(Users.class)))
                        .thenReturn(true);

                mockMvc.perform(post("/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isOk())
                       .andExpect(content().string("User registered successfully"));
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser
        @DisplayName("POST /register → 409 when user already exists (UserAlreadyExistsException)")
        void register_conflict_whenExists() {
            try {
                String email = "user@example.com";
                String json = "{\"email\":\"" + email + "\"}";

                when(userService.exists(email)).thenReturn(true);

                mockMvc.perform(post("/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isConflict());
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser
        @DisplayName("POST /register → 500 when registration fails (RuntimeException)")
        void register_failure_internalError() {
            try {
                String email = "user@example.com";
                String json = "{\"email\":\"" + email + "\"}";

                when(userService.exists(email)).thenReturn(false);
                when(userService.registerUser(org.mockito.ArgumentMatchers.any(Users.class)))
                        .thenReturn(false);

                mockMvc.perform(post("/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isInternalServerError());
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }
    }

    // ------------- LOGIN -----------------
    @Nested
    class LoginTests {

        @Test
        @DisplayName("POST /login → 200 and JWT token when authentication succeeds")
        void login_success() {
            try {
                String email = "user@example.com";
                String json = "{\"email\":\"" + email + "\",\"password\":\"pw\"}";
                String token = "JWT_TOKEN";

                when(userService.exists(email)).thenReturn(true);

                Authentication auth = mock(Authentication.class);
                when(auth.isAuthenticated()).thenReturn(true);
                when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any()))
                        .thenReturn(auth);

                UserDetails userDetails = mock(UserDetails.class);
                when(myUserDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
                when(jwtService.generateToken(userDetails)).thenReturn(token);

                mockMvc.perform(post("/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isOk())
                       .andExpect(content().string(token));
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @DisplayName("POST /login → 404 when user not found (UserNotFoundException)")
        void login_userNotFound() {
            try {
                String email = "nouser@example.com";
                String json = "{\"email\":\"" + email + "\",\"password\":\"x\"}";

                when(userService.exists(email)).thenReturn(false);

                mockMvc.perform(post("/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isNotFound());
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @DisplayName("POST /login → 401 when authentication fails (AuthenticationFailedException)")
        void login_authFails() {
            try {
                String email = "user@example.com";
                String json = "{\"email\":\"" + email + "\",\"password\":\"bad\"}";

                when(userService.exists(email)).thenReturn(true);

                Authentication auth = mock(Authentication.class);
                when(auth.isAuthenticated()).thenReturn(false);
                when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any()))
                        .thenReturn(auth);

                UserDetails userDetails = mock(UserDetails.class);
                when(myUserDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

                mockMvc.perform(post("/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isUnauthorized());
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }
    }

    // ------------- EMPLOYEE ENDPOINTS -----------------
    @Nested
    class EmployeeEndpoints {

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /viewProfile/{id} → 200 when found")
        void viewProfile_found() {
            try {
                Users u = new Users();
                when(userService.getProfile((long)1)).thenReturn(u);

                mockMvc.perform(get("/viewProfile/{id}", 1))
                       .andExpect(status().isOk())
                       .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /viewProfile/{id} → 404 when not found (UserNotFoundException)")
        void viewProfile_notFound() {
            try {
                when(userService.getProfile((long)999)).thenReturn(null);

                mockMvc.perform(get("/viewProfile/{id}", 999))
                       .andExpect(status().isNotFound());
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("DELETE /deleteProfile/{id} → 200 when deleted")
        void deleteProfile_ok() {
            try {
                when(userService.deleteUser(10)).thenReturn("Profile deleted successfully!");

                mockMvc.perform(delete("/deleteProfile/{id}", 10)
                                .with(csrf()))
                       .andExpect(status().isOk())
                       .andExpect(content().string("Profile deleted successfully!"));
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("DELETE /deleteProfile/{id} → 404 when user not found (UserNotFoundException)")
        void deleteProfile_notFound() {
            try {
                when(userService.deleteUser(777)).thenReturn("User not found!");

                mockMvc.perform(delete("/deleteProfile/{id}", 777)
                                .with(csrf()))
                       .andExpect(status().isNotFound());
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("PUT /updateProfile → 200 when updated")
        void updateProfile_ok() {
            try {
                when(userService.updateUser(org.mockito.ArgumentMatchers.any()))
                        .thenReturn(true);

                String json = "{\"email\":\"emp@example.com\",\"name\":\"Emp\",\"department\":\"Engg\",\"password\":\"pw\"}";

                mockMvc.perform(put("/updateProfile")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isOk())
                       .andExpect(content().string("User updated successfully!"));
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("PUT /updateProfile → 404 when updateUser returns false (UserNotFoundException)")
        void updateProfile_notExists() {
            try {
                when(userService.updateUser(org.mockito.ArgumentMatchers.any()))
                        .thenReturn(false);

                String json = "{\"email\":\"missing@example.com\"}";

                mockMvc.perform(put("/updateProfile")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isNotFound());
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }
    }

    // ------------- ADMIN ENDPOINTS -----------------
    @Nested
    class AdminEndpoints {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /addUser → 200 when not exists and registered")
        void addUser_ok() {
            try {
                String email = "new.admin@example.com";
                String json = "{\"email\":\"" + email + "\",\"password\":\"pw\"}";

                when(userService.exists(email)).thenReturn(false);
                when(userService.registerUser(org.mockito.ArgumentMatchers.any(Users.class)))
                        .thenReturn(true);

                mockMvc.perform(post("/addUser")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isOk())
                       .andExpect(content().string("Users registered successfully"));
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /addUser → 409 when user already exists (UserAlreadyExistsException)")
        void addUser_conflict() {
            try {
                String email = "exist@example.com";
                String json = "{\"email\":\"" + email + "\"}";

                when(userService.exists(email)).thenReturn(true);

                mockMvc.perform(post("/addUser")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isConflict());
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /addUser → 500 when registration fails (RuntimeException)")
        void addUser_fail() {
            try {
                String email = "fail@example.com";
                String json = "{\"email\":\"" + email + "\"}";

                when(userService.exists(email)).thenReturn(false);
                when(userService.registerUser(org.mockito.ArgumentMatchers.any(Users.class)))
                        .thenReturn(false);

                mockMvc.perform(post("/addUser")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isInternalServerError());
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /viewAllUsers → 200 with list")
        void viewAllUsers_ok() {
            try {
                when(userService.getUsers()).thenReturn(Arrays.asList(new Users(), new Users()));

                mockMvc.perform(get("/viewAllUsers"))
                       .andExpect(status().isOk())
                       .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                       .andExpect(jsonPath("$").isArray())
                       .andExpect(jsonPath("$[0]").exists())
                       .andExpect(jsonPath("$[1]").exists());
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /viewAllUsers → 200 with empty list")
        void viewAllUsers_empty() {
            try {
                when(userService.getUsers()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/viewAllUsers"))
                       .andExpect(status().isOk())
                       .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                       .andExpect(jsonPath("$").isArray())
                       .andExpect(jsonPath("$.length()").value(0));
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PUT /updateUserAdmin → 200 when updated")
        void updateUserAdmin_ok() {
            try {
                when(userService.updateUserAdmin(org.mockito.ArgumentMatchers.any()))
                        .thenReturn(true);

                String json = "{\"email\":\"adminupd@example.com\",\"role\":\"ADMIN\",\"status\":\"ACTIVE\",\"department\":\"QA\"}";

                mockMvc.perform(put("/updateUserAdmin")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isOk())
                       .andExpect(content().string("User updated successfully!"));
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PUT /updateUserAdmin → 404 when updateUserAdmin returns false (UserNotFoundException)")
        void updateUserAdmin_notExistsOrFail() {
            try {
                when(userService.updateUserAdmin(org.mockito.ArgumentMatchers.any()))
                        .thenReturn(false);

                String json = "{\"email\":\"missing@example.com\"}";

                mockMvc.perform(put("/updateUserAdmin")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                       .andExpect(status().isNotFound());
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE /deleteUserAdmin/{id} → 200 when deleted")
        void deleteUserAdmin_ok() {
            try {
                when(userService.deleteUser(55)).thenReturn("Profile deleted successfully!");

                mockMvc.perform(delete("/deleteUserAdmin/{id}", 55)
                                .with(csrf()))
                       .andExpect(status().isOk())
                       .andExpect(content().string("Profile deleted successfully!"));
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE /deleteUserAdmin/{id} → 404 when user not found (controller returns 404 body)")
        void deleteUserAdmin_notFound() {
            try {
                when(userService.deleteUser(404)).thenReturn("User not found!");

                mockMvc.perform(delete("/deleteUserAdmin/{id}", 404)
                                .with(csrf()))
                       .andExpect(status().isNotFound())
                       .andExpect(content().string("User not found!"));
            } catch (Exception e) {
                System.out.println("Exception occurred " + e);
            }
        }
    }
}