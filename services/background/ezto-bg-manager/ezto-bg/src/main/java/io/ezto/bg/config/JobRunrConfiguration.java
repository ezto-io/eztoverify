package io.ezto.bg.config;

import io.ezto.bg.helpers.EnvironmentReader;
import io.ezto.bg.utils.Constants;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.filters.RetryFilter;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.jobrunr.storage.sql.mysql.MySqlStorageProvider;
import org.jobrunr.storage.sql.oracle.OracleStorageProvider;
import com.mysql.cj.jdbc.MysqlDataSource;



import javax.sql.DataSource;

@Configuration
public class JobRunrConfiguration {

    @Bean
    public DataSource dataSource() throws Exception {
                String dbType = EnvironmentReader.getValue("JOB_DB_TYPE");
        if (dbType == null || dbType.isEmpty())
            throw new Exception("JOB_DB_TYPE setting is missing in configuration.");

        if (dbType.equals(Constants.ORACLE)) {
            oracle.jdbc.datasource.OracleDataSource ods = new oracle.jdbc.replay.OracleDataSourceImpl();
            ods.setURL(EnvironmentReader.getValue("JOB_DB_URL"));
            ods.setUser(EnvironmentReader.getValue("JOB_DB_USER"));
            ods.setPassword(EnvironmentReader.getValue("JOB_DB_PASSWORD"));
            return ods;
        }

        if (dbType.equals(Constants.MYSQL)) {
            MysqlDataSource msqlDs = new MysqlDataSource();
            msqlDs.setURL(EnvironmentReader.getValue("JOB_DB_URL"));
            msqlDs.setUser(EnvironmentReader.getValue("JOB_DB_USER"));
            msqlDs.setPassword(EnvironmentReader.getValue("JOB_DB_PASSWORD"));
            return msqlDs;
        }
 
        PGSimpleDataSource pgds = new PGSimpleDataSource();
        pgds.setUrl(EnvironmentReader.getValue("JOB_DB_URL"));
        pgds.setUser(EnvironmentReader.getValue("JOB_DB_USER"));
        pgds.setPassword(EnvironmentReader.getValue("JOB_DB_PASSWORD"));
        return pgds;

    }

    @Bean
    public StorageProvider storageProvider(DataSource dataSource) throws Exception {
                String dbType = EnvironmentReader.getValue("JOB_DB_TYPE");
        if (dbType == null || dbType.isEmpty())
            throw new Exception("JOB_DB_TYPE setting is missing in configuration.");

        if (dbType.equals(Constants.ORACLE))
            return new OracleStorageProvider(dataSource, Constants.TABLE_PREFIX, StorageProviderUtils.DatabaseOptions.CREATE);

        if (dbType.equals(Constants.MYSQL))
            return new MySqlStorageProvider(dataSource, Constants.TABLE_PREFIX, StorageProviderUtils.DatabaseOptions.CREATE);

        return new PostgresStorageProvider(dataSource, Constants.TABLE_PREFIX, StorageProviderUtils.DatabaseOptions.CREATE);
    }


    @Bean
    public org.jobrunr.configuration.JobRunrConfiguration.JobRunrConfigurationResult jobRunrConfigurationResult(StorageProvider storageProvider, ApplicationContext applicationContext) {
        return JobRunr.configure()
                .withJobFilter(new RetryFilter(Integer.parseInt(EnvironmentReader.getValue("JOB_RETRY_POLICY", "1"))))
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(Integer.parseInt(EnvironmentReader.getValue("JOB_WORKERS","10")))
                .useDashboard(8000)
                .initialize();
    }

    @Bean
    public JobScheduler jobScheduler(StorageProvider storageProvider) {
        return new JobScheduler(storageProvider);
    }
    //

}
