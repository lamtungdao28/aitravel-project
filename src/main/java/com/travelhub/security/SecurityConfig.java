package com.travelhub.security;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(@Value("${security.bcrypt-strength:12}") int strength){return new BCryptPasswordEncoder(strength);}
    @Bean AuthenticationManager authenticationManager(DatabaseUserDetailsService users,PasswordEncoder encoder) {
        var provider=new DaoAuthenticationProvider(users);provider.setPasswordEncoder(encoder);return new ProviderManager(provider);
    }
    @Bean JwtAuthenticationFilter jwtFilter(JwtService jwt,DatabaseUserDetailsService users,SecurityErrors errors) {
        return new JwtAuthenticationFilter(jwt,users,errors);
    }
    @Bean FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        var registration=new FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,JwtAuthenticationFilter filter,SecurityErrors errors) throws Exception {
        return http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable()).httpBasic(b -> b.disable())
                .authorizeHttpRequests(a -> a
                    .requestMatchers("/api/v1/auth/**","/actuator/health","/actuator/health/**","/swagger-ui.html","/swagger-ui/**","/v3/api-docs/**","/error").permitAll()
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET,"/api/v1/destinations/**","/api/v1/categories/**","/api/v1/places/**","/api/v1/reviews/**").permitAll()
                    .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> errors.write(req,res,HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler((req,res,ex) -> errors.write(req,res,HttpStatus.FORBIDDEN)))
                .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();
    }
}
