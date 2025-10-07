package com.aurionpro.papms.config;

import com.aurionpro.papms.batch.CsvEmployeeFieldSetMapper;
import com.aurionpro.papms.batch.EmployeeCsvItemProcessor;
import com.aurionpro.papms.batch.EmployeeCsvItemWriter;
import com.aurionpro.papms.batch.JobCompletionNotificationListener;
import com.aurionpro.papms.dto.CsvEmployeeRecord;
import com.aurionpro.papms.entity.Employee;
import com.aurionpro.papms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;

@Configuration
//@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig {


//    private final JobRepository jobRepository;
//    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    // Dependencies for processors and writers
    private final AppUserRepository appUserRepository;
    private final EmployeeRepository employeeRepository;
    private final BankAccountRepository bankAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.aurionpro.papms.emails.EmailService emailService;
    private final SalaryStructureRepository salaryStructureRepository;

//    @Bean
//    BatchDataSourceScriptDatabaseInitializer batchDataSourceInitializer(DataSource dataSource,
//                                                                        BatchProperties properties) {
//        return new BatchDataSourceScriptDatabaseInitializer(dataSource, properties.getJdbc());
//    }
    // 1. Reader: Reads CsvEmployeeRecord objects from the CSV file
//    @Bean
//    @StepScope
//    public FlatFileItemReader<CsvEmployeeRecord> employeeCsvReader(
//            @Value("#{jobParameters['filePath']}") String filePath) {
//
//        return new FlatFileItemReaderBuilder<CsvEmployeeRecord>()
//                .name("employeeCsvReader")
//                .resource(new FileSystemResource(filePath))
//                .linesToSkip(1) // Skip header row
//                .delimited()
//                .names("username", "password", "fullName", "email", "employeeCode",
//                        "dateOfJoining", "department", "jobTitle", "accountHolderName",
//                        "accountNumber", "bankName", "ifscCode", "basicSalary", "hra",
//                        "da", "pfContribution", "otherAllowances", "effectiveFromDate")
//                .fieldSetMapper(new BeanWrapperFieldSetMapper<CsvEmployeeRecord>() {{
//                    setTargetType(CsvEmployeeRecord.class);
//                }})
//                .build();
//    }
    @Bean
    @StepScope
    public FlatFileItemReader<CsvEmployeeRecord> employeeCsvReader(
            @Value("#{jobParameters['filePath']}") String filePath) {

        return new FlatFileItemReaderBuilder<CsvEmployeeRecord>()
                .name("employeeCsvReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1) // Skip header row
                .delimited()
                .names("username", "password", "fullName", "email", "employeeCode",
                        "dateOfJoining", "department", "jobTitle", "accountHolderName",
                        "accountNumber", "bankName", "ifscCode", "basicSalary", "hra",
                        "da", "pfContribution", "otherAllowances", "effectiveFromDate")
                .fieldSetMapper(new CsvEmployeeFieldSetMapper()) // Use custom mapper
                .build();
    }

    // 2. Processor with dependencies injected
    @Bean
    @StepScope
    public EmployeeCsvItemProcessor employeeCsvProcessor() {
        // Now we can use the constructor
        return new EmployeeCsvItemProcessor(
                appUserRepository,
                bankAccountRepository,
                passwordEncoder
        );
    }
//    @Bean
//    @StepScope
//    public EmployeeCsvItemProcessor employeeCsvProcessor() {
//        EmployeeCsvItemProcessor processor = new EmployeeCsvItemProcessor();
//        processor.setAppUserRepository(appUserRepository);
//        processor.setEmployeeRepository(employeeRepository);
//        processor.setBankAccountRepository(bankAccountRepository);
//        processor.setPasswordEncoder(passwordEncoder);
//        return processor;
//    }

    // 3. JPA Writer for Employee entities
    @Bean
    public JpaItemWriter<Employee> employeeJpaWriter() {
        JpaItemWriter<Employee> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    // 4. Custom Writer (for emails and organization linking) with dependencies
    @Bean
    @StepScope
    public EmployeeCsvItemWriter customEmployeeWriter(
            @Value("#{jobParameters['organizationId']}") Integer organizationId) {
        EmployeeCsvItemWriter writer = new EmployeeCsvItemWriter();
        writer.setOrganizationRepository(organizationRepository);
        writer.setEmployeeRepository(employeeRepository);
        writer.setAppUserRepository(appUserRepository);
        writer.setBankAccountRepository(bankAccountRepository);
        writer.setSalaryStructureRepository(salaryStructureRepository);
        writer.setEmailService(emailService);
        writer.setOrganizationId(organizationId);
        return writer;
    }

    // 5. Step Configuration
//    @Bean
//    public Step employeeCsvImportStep(FlatFileItemReader<CsvEmployeeRecord> employeeCsvReader,
//                                      EmployeeCsvItemProcessor employeeCsvProcessor,
//                                      EmployeeCsvItemWriter customEmployeeWriter) {
//
//        return new StepBuilder("employeeCsvImportStep", jobRepository)
//                .<CsvEmployeeRecord, Employee>chunk(10, transactionManager)
//                .reader(employeeCsvReader)
//                .processor(employeeCsvProcessor)
//                .writer(customEmployeeWriter)
//                .build();
//    }
    @Bean
    public Step employeeCsvImportStep(JobRepository jobRepository, // <-- Accept here
                                      PlatformTransactionManager transactionManager, // <-- Accept here
                                      FlatFileItemReader<CsvEmployeeRecord> employeeCsvReader,
                                      EmployeeCsvItemProcessor employeeCsvProcessor,
                                      EmployeeCsvItemWriter customEmployeeWriter) {

        return new StepBuilder("employeeCsvImportStep", jobRepository)
                .<CsvEmployeeRecord, Employee>chunk(10, transactionManager)
                .reader(employeeCsvReader)
                .processor(employeeCsvProcessor)
                .writer(customEmployeeWriter)
                .build();
    }


    // 6. Job Configuration
//    @Bean
//    public Job employeeCsvImportJob(Step employeeCsvImportStep,
//                                    JobCompletionNotificationListener listener) {
//
//        return new JobBuilder("employeeCsvImportJob", jobRepository)
//                .incrementer(new RunIdIncrementer())
//                .listener(listener)
//                .start(employeeCsvImportStep)
//                .build();
//    }
    @Bean
    public Job employeeCsvImportJob(JobRepository jobRepository, // <-- Accept here
                                    Step employeeCsvImportStep,
                                    JobCompletionNotificationListener listener) {

        return new JobBuilder("employeeCsvImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(employeeCsvImportStep)
                .build();
    }

}