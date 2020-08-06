package uk.ac.ebi.biosamples.ebeye.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class EbEyeBioSamplesDataDumpGeneratorConfig {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Bean("eraDataSource")
    @ConfigurationProperties(prefix="spring.datasource.erapro")
    public DataSource getEraDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("eraJdbcTemplate")
    public JdbcTemplate getEraJdbcTemplate(@Qualifier("eraDataSource") DataSource eraDataSource) {
        JdbcTemplate jdbc = new JdbcTemplate(eraDataSource);
        jdbc.setFetchSize(1000);
        return jdbc;
    }
}
