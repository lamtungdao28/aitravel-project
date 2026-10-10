package com.travelhub.security;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final DatabaseUserDetailsService users;
    private final SecurityErrors errors;
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String header=request.getHeader("Authorization");
        if(header!=null && header.regionMatches(true,0,"Bearer ",0,7)) {
            try {
                var principal=users.loadById(jwt.userId(header.substring(7)));
                var authentication=new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch(JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
                SecurityContextHolder.clearContext();errors.write(request,response,HttpStatus.UNAUTHORIZED);return;
            }
        }
        chain.doFilter(request,response);
    }
}
