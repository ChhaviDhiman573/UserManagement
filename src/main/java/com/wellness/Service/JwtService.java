package com.wellness.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.wellness.exception.AuthenticationFailedException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	
	
	private String secretKey;
	
	//constructor to initialize secret key
	public JwtService() {
		secretKey = "123456789012345678901234567890123456"; 
	}

	public String generateToken(UserDetails userDetails) {
		// to store jwt payload
		Map<String, Object> claims = new HashMap<>();
		claims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
		
		//construct jwt step by step
		return Jwts.builder()
				//adds payload data
				.setClaims(claims)
				
				// stores logged in user name
				.setSubject(userDetails.getUsername())
				
				// set issued time
				.setIssuedAt(new Date(System.currentTimeMillis()))
				
				// set expiration time for token here its 50 mins
				.setExpiration(new Date(System.currentTimeMillis()+1000*60*50)) 
				
				//to ensure token integrity(the token is not changed)
				//compact() - creates final jwt string
				.signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)				
				.compact();
	}

	private Key getKey() {
		return Keys.hmacShaKeyFor(
				secretKey.getBytes(StandardCharsets.UTF_8)
		);
	}

	public String extractUserName(String token) {
		try {
			return extractClaim(token, Claims::getSubject);
		}
		catch(Exception e) {
			throw new AuthenticationFailedException("Invalid JWT token");
		}
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimResolver) { 
		final Claims claims = extractAllClaims(token); 
		return claimResolver.apply(claims); 
	}
	private Claims extractAllClaims(String token) { 
		return Jwts.parserBuilder() 
		.setSigningKey(getKey()) 
		.build() 
		.parseClaimsJws(token) 
		.getBody(); 
	} 

	public boolean validateToken(String token, UserDetails userDetails) { 
		try {
			final String userName = extractUserName(token); 
			return (userName.equals(userDetails.getUsername()) && 
			!isTokenExpired(token)); 
		}
		catch(Exception e) {
			throw new AuthenticationFailedException("Invalid JWT token");
		}
		
	} 
	private boolean isTokenExpired(String token) { 
		return extractExpiration(token).before(new Date()); 
	}
	private Date extractExpiration(String token) { 
		return extractClaim(token, Claims::getExpiration); 
	}
}
