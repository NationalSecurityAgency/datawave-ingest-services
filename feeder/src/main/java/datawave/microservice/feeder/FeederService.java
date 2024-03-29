package datawave.microservice.feeder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Feeder's job is to monitor a directory for new files, move those new files to processing directory, and send a message to the rabbitmq exchange
 */
@EnableDiscoveryClient
@EnableScheduling
@SpringBootApplication(scanBasePackages = "datawave.microservice", exclude = {ErrorMvcAutoConfiguration.class})
public class FeederService {
    public static void main(String[] args) {
        SpringApplication.run(FeederService.class, args);
    }
}
