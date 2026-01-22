package com.approval.opsagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@ConfigurationPropertiesScan
@SpringBootApplication
public class ApprovalOpsAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApprovalOpsAgentApplication.class, args);
    }

}
