package com.wellness.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.wellness.Repository.IUserRepository;
import com.wellness.data.UserPrinciple;
import com.wellness.data.Users;
import com.wellness.exception.UserNotFoundException;

@Service
public class MyUserDetailsService implements UserDetailsService{
	@Autowired
	IUserRepository userRepo;
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Users user = userRepo.findByEmail(email);
		if(user==null) {
			throw new UserNotFoundException("User not found!");
		}
		return new UserPrinciple(user);
	}

}
