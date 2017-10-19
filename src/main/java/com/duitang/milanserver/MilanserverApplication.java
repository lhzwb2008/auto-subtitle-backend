package com.duitang.milanserver;

import com.duitang.milanserver.storage.StorageProperties;
import com.duitang.milanserver.storage.StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Created by zhangwenbo on 2017/9/13.
 */
@EnableAsync
@EnableConfigurationProperties(StorageProperties.class)
@SpringBootApplication
public class MilanserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(MilanserverApplication.class, args);
    }
    @Bean
    CommandLineRunner init(StorageService storageService) {
        return (args) -> {
            storageService.init();
//            storageService.deleteAll();
        };
    }
}
