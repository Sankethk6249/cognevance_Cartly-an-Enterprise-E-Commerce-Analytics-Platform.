package com.cognevance.ecommerce.config;

import com.cognevance.ecommerce.model.*;
import com.cognevance.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final ProductRepository products;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            User admin = new User();
            admin.setName("Admin"); admin.setEmail("admin@cartly.com");
            admin.setPassword(encoder.encode("Admin@123")); admin.setRole(User.Role.ADMIN);
            users.save(admin);
        }
        if (products.count() == 0) {
            products.saveAll(List.of(
                p("Wireless Headphones", "Over-ear Bluetooth headphones with 30-hour battery.", "2499", 40, "Electronics", "headphones"),
                p("Mechanical Keyboard", "Hot-swappable 75% keyboard with tactile switches.", "3999", 25, "Electronics", "keyboard"),
                p("Smart Watch", "Fitness tracking, heart-rate monitor and GPS.", "5499", 18, "Electronics", "watch"),
                p("Running Shoes", "Lightweight breathable shoes for daily runs.", "2999", 60, "Fashion", "shoes"),
                p("Denim Jacket", "Classic fit washed denim jacket.", "1999", 30, "Fashion", "jacket"),
                p("Ceramic Coffee Mug Set", "Set of 4 handmade ceramic mugs.", "799", 80, "Home", "mug"),
                p("Desk Lamp", "Dimmable LED lamp with USB charging port.", "1299", 3, "Home", "lamp"),
                p("Yoga Mat", "Non-slip 6 mm mat with carry strap.", "999", 50, "Fitness", "yoga")));
        }
    }

    private Product p(String name, String desc, String price, int stock, String cat, String seed) {
        Product p = new Product();
        p.setName(name); p.setDescription(desc); p.setPrice(new BigDecimal(price)); p.setStock(stock);
        p.setCategory(cat); p.setImageUrl("https://picsum.photos/seed/" + seed + "/400/300");
        return p;
    }
}