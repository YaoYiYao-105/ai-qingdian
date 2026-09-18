package com.aiqingdian;

import com.aiqingdian.config.ArkProperties;
import com.aiqingdian.config.CorrectionProperties;
import com.aiqingdian.config.CvProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ArkProperties.class, CvProperties.class, CorrectionProperties.class})
public class AiQingDianApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiQingDianApplication.class, args);
    }
}
