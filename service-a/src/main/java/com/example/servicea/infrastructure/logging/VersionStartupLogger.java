package com.example.servicea.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class VersionStartupLogger implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionStartupLogger.class);

    private final Environment environment;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    public VersionStartupLogger(
            Environment environment,
            ObjectProvider<BuildProperties> buildPropertiesProvider
    ) {
        this.environment = environment;
        this.buildPropertiesProvider = buildPropertiesProvider;
    }

    @Override
    public void run(ApplicationArguments args) {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        String applicationName = environment.getProperty("spring.application.name", "application");
        String version = buildProperties != null ? buildProperties.getVersion() : "unknown";
        String buildTime = buildProperties != null && buildProperties.getTime() != null
                ? buildProperties.getTime().toString()
                : "unknown";

        LOGGER.info("Application {} started with version {} (buildTime={})", applicationName, version, buildTime);
    }
}
