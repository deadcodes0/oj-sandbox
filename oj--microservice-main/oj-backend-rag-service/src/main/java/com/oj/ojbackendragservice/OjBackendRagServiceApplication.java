package com.oj.ojbackendragservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.oj")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.oj.ojbackendserviceclient.service"})
public class OjBackendRagServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjBackendRagServiceApplication.class, args);
    }

}
