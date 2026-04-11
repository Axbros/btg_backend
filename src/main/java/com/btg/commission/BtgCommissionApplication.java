package com.btg.commission;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.btg.commission.mapper")
public class BtgCommissionApplication {

    public static void main(String[] args) {
        SpringApplication.run(BtgCommissionApplication.class, args);
    }
}
