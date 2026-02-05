package com.wellness.controller;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.wellness.data.Users;
import com.wellness.dto.MyRequest;
import com.wellness.dto.UpdateUser;
import com.wellness.dto.UpdateUserAdmin;
import com.wellness.exception.AuthenticationFailedException;
import com.wellness.exception.UserAlreadyExistsException;
import com.wellness.exception.UserNotFoundException;
import com.wellness.exception.UserNotRegisteredException;
import com.wellness.service.JwtService;
import com.wellness.service.MyUserDetailsService;
import com.wellness.service.UserService;

import ch.qos.logback.classic.Logger;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MyController {
	
	public static final Logger loggerobj = (Logger) LoggerFactory.getLogger(MyController.class);
	
	private final UserService userService;
	@PostMapping("/register")
	public ResponseEntity<String> register(@RequestBody Users user){
		HttpStatus httpStatus= HttpStatus.OK;
		String msg="";
		boolean exist = userService.exists(user.getEmail());
		if(exist) {
			throw new UserAlreadyExistsException("User already exists!");
		}
		boolean result = userService.registerUser(user);
		if(!result) {
			throw new UserNotRegisteredException("User not registered!");
		}
		else {
			msg = "User registered successfully";
		}
		return ResponseEntity.status(httpStatus).body(msg);
		
	}

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final MyUserDetailsService myUserDetailsService;
	@PostMapping("/login")
	public ResponseEntity<String> login(@RequestBody MyRequest user) {
		boolean exist = userService.exists(user.getEmail());
		if(!exist) {
			throw new UserNotFoundException("User not found!");
		}
		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken
						(user.getEmail(), user.getPassword()));
		UserDetails userDetails = myUserDetailsService.loadUserByUsername(user.getEmail());
		if(authentication.isAuthenticated())
			return ResponseEntity.status(HttpStatus.OK).body(jwtService.generateToken(userDetails));
		else
			throw new AuthenticationFailedException("Login failed!");
	}

	@PreAuthorize("hasRole('EMPLOYEE')")
	@GetMapping("/viewProfile/{id}")
	public ResponseEntity<Users> viewProfile(@PathVariable Long id) {
		Users myuser = userService.getProfile(id);
		if(myuser==null) {
			throw new UserNotFoundException("User not found!");
		}
		return ResponseEntity.status(HttpStatus.OK).body(myuser);
	}

	@PreAuthorize("hasRole('EMPLOYEE')")
	@DeleteMapping("/deleteProfile/{id}")
	public ResponseEntity<String> deleteProfile(@PathVariable Long id){
		String result = userService.deleteUser(id);
		if(result.equalsIgnoreCase("User not found!")) {
			throw new UserNotFoundException("User not found!");
		}
		return ResponseEntity.status(HttpStatus.OK).body(result);
	}
	
	@PreAuthorize("hasRole('EMPLOYEE')")
	@PutMapping("/updateProfile")
	public ResponseEntity<String> updateProfile(@RequestBody UpdateUser user){
		boolean updated = userService.updateUser(user);
		if(!updated) {
			throw new UserNotFoundException("User not found!");
		}
		return ResponseEntity.status(HttpStatus.OK).body("User updated successfully!");
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/addUser")
	public ResponseEntity<String> addUser(@RequestBody Users user){
		HttpStatus httpStatus= HttpStatus.OK;
		String msg="";
		boolean exist = userService.exists(user.getEmail());
		if(exist) {
			throw new UserAlreadyExistsException("User already exists!");
		}
		boolean result = userService.registerUser(user);
		if(!result) {
			throw new UserNotRegisteredException("User not registered!");
		}
		else {
			msg = "Users registered successfully";
		}
		return ResponseEntity.status(httpStatus).body(msg);
		
	}
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/viewAllUsers")
	public ResponseEntity<List<Users>> viewAllUsers() {
		List<Users> users = userService.getUsers();
		return ResponseEntity.ok().body(users);
	}
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/updateUserAdmin")
	public ResponseEntity<String> updateUserAdmin(@RequestBody UpdateUserAdmin user){
		boolean updated = userService.updateUserAdmin(user);
		if(!updated) {
			throw new UserNotFoundException("User not found!");
		}
		return ResponseEntity.status(HttpStatus.OK).body("User updated successfully!");
	}
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/deleteUserAdmin/{id}")
	public ResponseEntity<String> deleteUserAdmin(@PathVariable Long id){
		String result = userService.deleteUser(id);
		if(result.equals("User not found!")) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
		}
		return ResponseEntity.status(HttpStatus.OK).body(result);
	}
}
