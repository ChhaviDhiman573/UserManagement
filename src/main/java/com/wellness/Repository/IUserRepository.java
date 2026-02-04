package com.wellness.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wellness.data.Users;

public interface IUserRepository extends JpaRepository<Users, Integer>{
	Users findByEmail(String email);
	boolean existsByEmail(String email);
}
