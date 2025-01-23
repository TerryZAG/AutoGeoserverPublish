package com.terryzag.AutoGeoserver.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Data
@Configuration
@ConfigurationProperties(prefix = "geoserver")
public class Geoserver {
    private URI url;
    private String username;
    private String password;
}
