package com.licoreria.pos;

import com.licoreria.pos.config.PosProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PosProperties.class)
public class PosLicoreriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PosLicoreriaApplication.class, args);
    }
}
