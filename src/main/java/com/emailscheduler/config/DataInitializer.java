package com.emailscheduler.config;

import com.emailscheduler.model.Role;
import com.emailscheduler.model.User;
import com.emailscheduler.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed default Admin account if not present
        if (userRepository.findByEmail("admin@emailscheduler.com").isEmpty()) {
            User admin = new User();
            admin.setName("System Administrator");
            admin.setEmail("admin@emailscheduler.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
            System.out.println("Default admin user seeded: admin@emailscheduler.com / admin123");
        }

        // Seed default User account if not present
        if (userRepository.findByEmail("user@emailscheduler.com").isEmpty()) {
            User user = new User();
            user.setName("John Doe");
            user.setEmail("user@emailscheduler.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRole(Role.USER);
            user.setEnabled(true);
            userRepository.save(user);
            System.out.println("Default user seeded: user@emailscheduler.com / user123");
        }
    }
}
