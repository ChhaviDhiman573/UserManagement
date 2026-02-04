package com.wellness.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.wellness.data.UserPrinciple;
import com.wellness.data.Users;
import com.wellness.exception.UserNotFoundException;
import com.wellness.repository.IUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService{
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
