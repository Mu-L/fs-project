package com.iisquare.fs.web.govern.dsconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean(name = "governDataSource")
    @Qualifier("governDataSource")
    @ConfigurationProperties(prefix="spring.datasource.govern")
    @Primary
    public DataSource governDataSource() {
        return DataSourceBuilder.create().build();
    }

}
