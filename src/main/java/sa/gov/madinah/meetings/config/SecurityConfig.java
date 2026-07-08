package sa.gov.madinah.meetings.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import sa.gov.madinah.meetings.security.LoginSuccessHandler;

/** إعدادات الأمان: تسجيل دخول، BCrypt، حماية الصفحات حسب الصلاحية. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;

    public SecurityConfig(LoginSuccessHandler loginSuccessHandler) {
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/fonts/**", "/images/**",
                        "/webjars/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // إدارة المستخدمين والجهات واعتماد الاستيراد لمدير النظام فقط
                .requestMatchers("/users/**").hasRole("ADMIN")
                .requestMatchers("/departments/new", "/departments/*/edit", "/departments/*/toggle",
                        "/departments/save").hasRole("ADMIN")
                .requestMatchers("/audit/**").hasRole("ADMIN")
                .requestMatchers("/import/approve/**", "/import/reject/**").hasRole("ADMIN")
                .requestMatchers("/import/**").hasAnyRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(loginSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
            // للسماح بعمل لوحة H2 داخل إطار
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // استثناء لوحة H2 من حماية CSRF
            .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")));

        return http.build();
    }
}
