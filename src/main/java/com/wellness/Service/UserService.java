package com.wellness.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.wellness.Repository.IUserRepository;
import com.wellness.data.Users;
import com.wellness.dto.UpdateUser;
import com.wellness.dto.UpdateUserAdmin;
import com.wellness.exception.UserAlreadyExistsException;
import com.wellness.exception.UserNotFoundException;

@Service
public class UserService {
	
	@Autowired
	IUserRepository userRepository;
	
	@Autowired
	PasswordEncoder encoder;
	public boolean registerUser(Users user) {
		if(userRepository.existsByEmail(user.getEmail())) {
			throw new UserAlreadyExistsException("User already exists");
		}
		user.setPassword(encoder.encode(user.getPassword()));
		return userRepository.save(user) != null;
	}
	public Users getProfile(Integer id) {
		Users profile = userRepository.findById(id).orElse(null);
		if(profile==null) {
			throw new UserNotFoundException("User not found");
		}
		return userRepository.findById(id).orElse(null);
	}
	public List<Users> getUsers(){
		return userRepository.findAll();
	}
	public String deleteUser(int id) {
		Users user = userRepository.findById(id).orElse(null);
		if(user==null) {
			return "User not found!";
		}
		userRepository.deleteById(id);
		return "Profile deleted successfully!";
	}
	public boolean exists(String email) {
		return userRepository.existsByEmail(email);
	}
	public boolean updateUser(UpdateUser user) {
		Users myuser = userRepository.findByEmail(user.getEmail());
		if(myuser==null) {
			throw new UserNotFoundException("User not found");
		}
		myuser.setName(user.getName());
		myuser.setDepartment(user.getDepartment());
		myuser.setPassword(encoder.encode(user.getPassword()));
		return userRepository.save(myuser) != null;
	}
	public boolean updateUserAdmin(UpdateUserAdmin user) {
		Users myuser = userRepository.findByEmail(user.getEmail());
		if(myuser==null) {
			throw new UserNotFoundException("User not found");
		}
		myuser.setStatus(user.getStatus());
		myuser.setDepartment(user.getDepartment());
		myuser.setRole(user.getRole());
		return userRepository.save(myuser) != null;
	}
}
