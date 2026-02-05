package com.wellness.data;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="users")
public class Users {
	@Id
	@Column(name="user_id")
	@GeneratedValue (strategy=GenerationType.IDENTITY)
	private Long userId;
	
	@Column(name="name", nullable=false)
	private String name;
	
	@Column(name="email", nullable=false)
	private String email;
	
	@Column(name="password", nullable=false)
	private String password;
	
	@Column(name="department", nullable=false)
	private String department;
	
	@Column(name="manager_id", nullable=true)
	private Integer managerId;
	
	@Enumerated(EnumType.STRING)
	@Column(name="role", nullable=false)
	private Role role;
	
	@Enumerated(EnumType.STRING)
	@Column(name="status", nullable=false)
	private Status status;

	@CreationTimestamp
	@Column(name="created_at", updatable=false)
	private LocalDateTime createdAt;
	
	@PrePersist
	public void prePersist() {
		if(email==null || password==null) {
			throw new IllegalArgumentException("Email or password cannot be null");
		}
	}
	
}
