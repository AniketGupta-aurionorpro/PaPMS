//package com.aurionpro.papms.config;
//
//import com.aurionpro.papms.batch.CsvEmployeeFieldSetMapper;
//import com.aurionpro.papms.batch.EmployeeCsvItemProcessor;
//import com.aurionpro.papms.batch.EmployeeCsvItemWriter;
//import com.aurionpro.papms.batch.JobCompletionNotificationListener;
//import com.aurionpro.papms.dto.CsvEmployeeRecord;
//import com.aurionpro.papms.entity.Employee;
//import com.aurionpro.papms.entity.Organization;
//import com.aurionpro.papms.exception.NotFoundException;
//import com.aurionpro.papms.repository.*;
//import com.aurionpro.papms.service.EmployeeServiceImpl;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.launch.support.RunIdIncrementer;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.batch.item.ItemProcessor;
//import org.springframework.batch.item.ItemWriter;
//import org.springframework.batch.item.database.JpaItemWriter;
//import org.springframework.batch.item.file.FlatFileItemReader;
//import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
//import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
//import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.FileSystemResource;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.batch.core.configuration.annotation.StepScope;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.batch.item.support.CompositeItemWriter;
//import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
//import org.springframework.boot.autoconfigure.batch.BatchProperties;
//import org.springframework.boot.jdbc.DataSourceBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;
//import javax.sql.DataSource;
//import jakarta.persistence.EntityManagerFactory;
//
//import java.util.List;
//
//
//@Configuration
////@EnableBatchProcessing
//@RequiredArgsConstructor
//public class BatchConfig {
//
//    private static final Logger log = LoggerFactory.getLogger(BatchConfig.class);
//    private final JobRepository jobRepository;
////    private final PlatformTransactionManager transactionManager;
//    private final EntityManagerFactory entityManagerFactory;
//
//    // Dependencies for processors and writers
//    private final AppUserRepository appUserRepository;
//    private final EmployeeRepository employeeRepository;
//    private final BankAccountRepository bankAccountRepository;
//    private final OrganizationRepository organizationRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final com.aurionpro.papms.emails.EmailService emailService;
//    private final SalaryStructureRepository salaryStructureRepository;
//
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
//                .fieldSetMapper(new CsvEmployeeFieldSetMapper()) // Use custom mapper
//                .build();
//    }
//
//    @Bean
//    @StepScope
//    public ItemProcessor<Employee, Employee> employeeOrganizationProcessor(
//            @Value("#{jobParameters['organizationId']}") Integer organizationId) {
//        Organization organization = organizationRepository.findById(organizationId)
//                .orElseThrow(() -> new NotFoundException("Organization not found for batch job: " + organizationId));
//
//        return employee -> {
//            employee.setOrganization(organization);
//            employee.getUser().setOrganizationId(organization.getId());
//            return employee;
//        };
//    }
//    @Bean
//    public JpaItemWriter<Employee> employeeJpaWriter() {
//        return new JpaItemWriterBuilder<Employee>()
//                .entityManagerFactory(entityManagerFactory)
//                .build();
//    }
//
//    // 2. Processor with dependencies injected
//    @Bean
//    @StepScope
//    public EmployeeCsvItemProcessor employeeCsvProcessor() {
//        // Now we can use the constructor
//        return new EmployeeCsvItemProcessor(
//                appUserRepository,
//                bankAccountRepository,
//                passwordEncoder
//        );
//    }
//
//
//    // 3. JPA Writer for Employee entities
////    @Bean
////    public JpaItemWriter<Employee> employeeJpaWriter() {
////        JpaItemWriter<Employee> writer = new JpaItemWriter<>();
////        writer.setEntityManagerFactory(entityManagerFactory);
////        return writer;
////    }
//
//    // 4. Custom Writer (for emails and organization linking) with dependencies
//    @Bean
//    @StepScope
//    public EmployeeCsvItemWriter customEmployeeWriter(
//            @Value("#{jobParameters['organizationId']}") Integer organizationId) {
//        EmployeeCsvItemWriter writer = new EmployeeCsvItemWriter();
//        writer.setOrganizationRepository(organizationRepository);
//        writer.setEmployeeRepository(employeeRepository);
//        writer.setAppUserRepository(appUserRepository);
//        writer.setBankAccountRepository(bankAccountRepository);
//        writer.setSalaryStructureRepository(salaryStructureRepository);
//        writer.setEmailService(emailService);
//        writer.setOrganizationId(organizationId);
//        return writer;
//    }
//
//    // 5. Step Configuration
////    @Bean
////    public Step employeeCsvImportStep(FlatFileItemReader<CsvEmployeeRecord> employeeCsvReader,
////                                      EmployeeCsvItemProcessor employeeCsvProcessor,
////                                      EmployeeCsvItemWriter customEmployeeWriter) {
////
////        return new StepBuilder("employeeCsvImportStep", jobRepository)
////                .<CsvEmployeeRecord, Employee>chunk(10, transactionManager)
////                .reader(employeeCsvReader)
////                .processor(employeeCsvProcessor)
////                .writer(customEmployeeWriter)
////                .build();
////    }
//    @Bean
//    public Step employeeCsvImportStep(JobRepository jobRepository, // <-- Accept JobRepository here
//                                      PlatformTransactionManager transactionManager, // <-- Accept TransactionManager here
//                                      FlatFileItemReader<CsvEmployeeRecord> employeeCsvReader,
//                                      EmployeeCsvItemProcessor employeeCsvProcessor,
//                                      EmployeeCsvItemWriter customEmployeeWriter) {
//
//        return new StepBuilder("employeeCsvImportStep", jobRepository) // Use the injected jobRepository
//                .<CsvEmployeeRecord, Employee>chunk(10, transactionManager) // Use the injected transactionManager
//                .reader(employeeCsvReader)
//                .processor(employeeCsvProcessor)
//                .writer(customEmployeeWriter)
//                .build();
//    }
//
//
//    // 6. Job Configuration
////    @Bean
////    public Job employeeCsvImportJob(Step employeeCsvImportStep,
////                                    JobCompletionNotificationListener listener) {
////
////        return new JobBuilder("employeeCsvImportJob", jobRepository)
////                .incrementer(new RunIdIncrementer())
////                .listener(listener)
////                .start(employeeCsvImportStep)
////                .build();
////    }
//    @Bean
//    public Job employeeCsvImportJob(JobRepository jobRepository, // <-- Accept JobRepository here
//                                    Step employeeCsvImportStep,  // <-- Spring injects the Step bean defined above
//                                    JobCompletionNotificationListener listener) {
//
//        return new JobBuilder("employeeCsvImportJob", jobRepository) // Use the injected jobRepository
//                .incrementer(new RunIdIncrementer())
//                .listener(listener)
//                .flow(employeeCsvImportStep) // Using .flow() is slightly more conventional than .start()
//                .end()
//                .build();
//    }
//
//    @Bean
//    @StepScope
//    public ItemWriter<Employee> employeeEmailWriter(
//            @Value("#{jobParameters['organizationId']}") Integer organizationId) {
//
//        Organization organization = organizationRepository.findById(organizationId)
//                .orElseThrow(() -> new NotFoundException("Organization not found for batch job: " + organizationId));
//
//        // This is a simple lambda-based ItemWriter
//        return chunk -> {
//            for (Employee employee : chunk) {
//                try {
//                    String subject = "Welcome to " + organization.getCompanyName();
//                    String body = String.format("""
//                    <h3>Hello %s,</h3>
//                    <p>Your employee account has been created successfully via bulk upload.</p>
//                    <p><b>Username:</b> %s</p>
//                    """, employee.getUser().getFullName(), employee.getUser().getUsername());
//
//                    emailService.sendEmail(organization.getContactEmail(), employee.getUser().getEmail(), subject, body);
//                } catch (Exception e) {
//                    log.warn("Failed to send welcome email to {}", employee.getUser().getEmail(), e);
//                }
//            }
//        };
//    }
//
//    @Bean
//    public CompositeItemWriter<Employee> compositeEmployeeWriter(
//            JpaItemWriter<Employee> employeeJpaWriter,
//            ItemWriter<Employee> employeeEmailWriter
//    ) {
//        CompositeItemWriter<Employee> writer = new CompositeItemWriter<>();
//        writer.setDelegates(List.of(employeeJpaWriter, employeeEmailWriter));
//        return writer;
//    }
//
//    @Bean
//    public Step employeeCsvImportStep(JobRepository jobRepository,
//                                      PlatformTransactionManager transactionManager,
//                                      FlatFileItemReader<CsvEmployeeRecord> employeeCsvReader,
//                                      EmployeeCsvItemProcessor employeeCsvProcessor,
//                                      ItemProcessor<Employee, Employee> employeeOrganizationProcessor,
//                                      CompositeItemWriter<Employee> compositeEmployeeWriter) { // <-- Use composite
//
//        return new StepBuilder("employeeCsvImportStep", jobRepository)
//                .<CsvEmployeeRecord, Employee>chunk(500, transactionManager) // Increased chunk size
//                .reader(employeeCsvReader)
//                .processor(new CompositeItemProcessorBuilder<CsvEmployeeRecord, Employee>()
//                        .delegates(employeeCsvProcessor, employeeOrganizationProcessor)
//                        .build())
//                .writer(compositeEmployeeWriter) // <-- Use composite
//                .listener(employeeCsvProcessor)
//                .build();
//    }
//
//}

// java/com/aurionpro/papms/config/BatchConfig.java

package com.aurionpro.papms.config;

import com.aurionpro.papms.batch.CsvEmployeeFieldSetMapper;
import com.aurionpro.papms.batch.EmployeeCsvItemProcessor;
import com.aurionpro.papms.batch.JobCompletionNotificationListener;
import com.aurionpro.papms.dto.CsvEmployeeRecord;
import com.aurionpro.papms.entity.Employee;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.*;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {

    // Dependencies are injected via the constructor
    private final EntityManagerFactory entityManagerFactory;
    private final AppUserRepository appUserRepository;
    private final BankAccountRepository bankAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.aurionpro.papms.emails.EmailService emailService;

    // =================================================================
    // 1. READER
    // =================================================================
    @Bean
    @StepScope
    public FlatFileItemReader<CsvEmployeeRecord> employeeCsvReader(
            @Value("#{jobParameters['filePath']}") String filePath) {
        return new FlatFileItemReaderBuilder<CsvEmployeeRecord>()
                .name("employeeCsvReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .delimited()
                .names("username", "password", "fullName", "email", "employeeCode",
                        "dateOfJoining", "department", "jobTitle", "accountHolderName",
                        "accountNumber", "bankName", "ifscCode", "basicSalary", "hra",
                        "da", "pfContribution", "otherAllowances", "effectiveFromDate")
                .fieldSetMapper(new CsvEmployeeFieldSetMapper())
                .build();
    }

    // =================================================================
    // 2. PROCESSORS
    // =================================================================
    @Bean
    @StepScope
    public EmployeeCsvItemProcessor employeeCsvProcessor() {
        // Processor for validation and transformation from CsvRecord to Employee
        return new EmployeeCsvItemProcessor(
                appUserRepository,
                bankAccountRepository,
                passwordEncoder
        );
    }

    @Bean
    @StepScope
    public ItemProcessor<Employee, Employee> employeeOrganizationProcessor(
            @Value("#{jobParameters['organizationId']}") Integer organizationId) {
        // Processor to find the organization once and attach it to each Employee entity
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found for batch job: " + organizationId));

        return employee -> {
            employee.setOrganization(organization);
            employee.getUser().setOrganizationId(organization.getId());
            return employee;
        };
    }

    // =================================================================
    // 3. WRITERS (Efficient, separated logic)
    // =================================================================
    @Bean
    public JpaItemWriter<Employee> employeeJpaWriter() {
        // Highly optimized writer for saving JPA entities in bulk
        return new JpaItemWriterBuilder<Employee>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    @Bean
    @StepScope
    public ItemWriter<Employee> employeeEmailWriter(
            @Value("#{jobParameters['organizationId']}") Integer organizationId) {
        // Dedicated writer for sending emails AFTER the data has been committed to the DB
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found for batch job: " + organizationId));

        return chunk -> {
            for (Employee employee : chunk) {
                try {
                    String subject = "Welcome to " + organization.getCompanyName();
                    String body = String.format("""
                        <h3>Hello %s,</h3>
                        <p>Your employee account has been created successfully via bulk upload.</p>
                        <p><b>Username:</b> %s</p>
                        """, employee.getUser().getFullName(), employee.getUser().getUsername());

                    emailService.sendEmail(organization.getContactEmail(), employee.getUser().getEmail(), subject, body);
                } catch (Exception e) {
                    log.warn("Failed to send welcome email to {}", employee.getUser().getEmail(), e);
                }
            }
        };
    }

    @Bean
    public CompositeItemWriter<Employee> compositeEmployeeWriter(
            JpaItemWriter<Employee> employeeJpaWriter,
            ItemWriter<Employee> employeeEmailWriter
    ) {
        // Combines the DB writer and Email writer to run in sequence for each chunk
        CompositeItemWriter<Employee> writer = new CompositeItemWriter<>();
        writer.setDelegates(List.of(employeeJpaWriter, employeeEmailWriter));
        return writer;
    }

    // =================================================================
    // 4. THE CORRECTED STEP CONFIGURATION (The main fix)
    // =================================================================
    @Bean
    public Step employeeCsvImportStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      FlatFileItemReader<CsvEmployeeRecord> employeeCsvReader,
                                      EmployeeCsvItemProcessor employeeCsvProcessor,
                                      ItemProcessor<Employee, Employee> employeeOrganizationProcessor,
                                      CompositeItemWriter<Employee> compositeEmployeeWriter) {

        return new StepBuilder("employeeCsvImportStep", jobRepository)
                .<CsvEmployeeRecord, Employee>chunk(50, transactionManager) // Increased chunk size for performance
                .reader(employeeCsvReader)
                .processor(new CompositeItemProcessorBuilder<CsvEmployeeRecord, Employee>()
                        .delegates(employeeCsvProcessor, employeeOrganizationProcessor)
                        .build())
                .writer(compositeEmployeeWriter) // Use the efficient composite writer
                .listener(employeeCsvProcessor) // Add listener for @BeforeStep data loading
                .build();
    }

    // =================================================================
    // 5. JOB CONFIGURATION
    // =================================================================
    @Bean
    public Job employeeCsvImportJob(JobRepository jobRepository,
                                    Step employeeCsvImportStep,
                                    JobCompletionNotificationListener listener) {
        return new JobBuilder("employeeCsvImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(employeeCsvImportStep)
                .end()
                .build();
    }
}