package com.wellness.configurations;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.wellness.Service.JwtService;
import com.wellness.Service.MyUserDetailsService;
import com.wellness.exception.AuthenticationFailedException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilters extends OncePerRequestFilter{
	@Autowired
	JwtService jwtService;
	@Autowired
	ApplicationContext context;
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			String path = request.getServletPath();
			if(path.equals("/login") || path.equals("/register")) {		
				//to continue the filter chain
				filterChain.doFilter(request, response);
				return;
			}
			
			String authHeader = request.getHeader("Authorization");
			String username = null;
			String token = null;
			
			if(authHeader!=null && authHeader.startsWith("Bearer ")) {
				token = authHeader.substring(7);
				username = jwtService.extractUserName(token);
			}
			if(username!=null && SecurityContextHolder.getContext().getAuthentication()==null) {
				UserDetails userDetails = context.getBean(MyUserDetailsService.class).loadUserByUsername(username);
				if(jwtService.validateToken(token, userDetails)) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
					
				}
			}
			filterChain.doFilter(request, response);
		}
		catch(Exception e) {
			throw new AuthenticationFailedException("Invalid or expired token");
		}
	}

}
