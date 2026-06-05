package com.menicucci.catalogo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.menicucci.catalogo.model.Usuario;
import com.menicucci.catalogo.service.UsuarioService;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String LOGIN_PATH = "/login";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        try {
            http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                        "/",
                        "/index",
                        "/index.html",
                        "/home",
                        "/comprar/**",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/uploads/**",
                        "/favicon.ico",
                        LOGIN_PATH
                    ).permitAll()
                    .requestMatchers(HttpMethod.POST, "/**").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/**").authenticated()
                    .anyRequest().authenticated()
                )
                .formLogin(form -> form
                    .loginPage(LOGIN_PATH)
                    .loginProcessingUrl(LOGIN_PATH)
                    .successHandler(authenticationSuccessHandler())
                    .failureUrl("/login?error=true")
                    .permitAll()
                )
                .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout=true")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(LOGIN_PATH)))
                .rememberMe(Customizer.withDefaults());

            return http.build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build security filter chain.", ex);
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
        public UserDetailsService userDetailsService(UsuarioService usuarioService) {
        return username -> usuarioService.buscarPorUsername(username)
                    .map(this::toUserDetails)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
            return (request, response, authentication) -> response.sendRedirect("/admin");
    }

        private org.springframework.security.core.userdetails.UserDetails toUserDetails(Usuario usuario) {
            return User.withUsername(usuario.getUsername())
                    .password(usuario.getPassword())
                    .roles("USER")
                    .build();
        }

}
