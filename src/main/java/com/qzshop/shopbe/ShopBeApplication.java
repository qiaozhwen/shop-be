package com.qzshop.shopbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.qzshop.shopbe")
public class ShopBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopBeApplication.class, args);
    }
}
