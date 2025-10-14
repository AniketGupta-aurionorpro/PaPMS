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
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final AppUserRepository appUserRepository;
    private final BankAccountRepository bankAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.aurionpro.papms.emails.EmailService emailService;

//    @Bean
//    public TaskExecutor taskExecutor() {
//        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
//        asyncTaskExecutor.setConcurrencyLimit(10); // <-- This is the modern replacement for throttleLimit
//        asyncTaskExecutor.setThreadNamePrefix("spring_batch-");
//        return asyncTaskExecutor;
//    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // Number of threads to keep in the pool
        executor.setMaxPoolSize(10);  // Maximum number of threads
        executor.setQueueCapacity(100); // How many tasks can wait in line
        executor.setThreadNamePrefix("batch-thread-");
        executor.initialize();
        return executor;
    }

    // ================== 1. READER ==================
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

    // ================== 2. PROCESSORS ==================
    @Bean
    @StepScope
    public EmployeeCsvItemProcessor employeeCsvProcessor() {
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
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found for batch job: " + organizationId));

        return employee -> {
            if (employee != null) {
                employee.setOrganization(organization);
                employee.getUser().setOrganizationId(organization.getId());
            }
            return employee;
        };
    }

    // ================== 3. WRITERS ==================
    @Bean
    public JpaItemWriter<Employee> employeeJpaWriter() {
        return new JpaItemWriterBuilder<Employee>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    @Bean
    @StepScope
    public ItemWriter<Employee> employeeEmailWriter(
            @Value("#{jobParameters['organizationId']}") Integer organizationId) {
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
                        <p>Please use the temporary password from the CSV file to log in.</p>
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
        CompositeItemWriter<Employee> writer = new CompositeItemWriter<>();
        writer.setDelegates(List.of(employeeJpaWriter, employeeEmailWriter));
        return writer;
    }

    // ================== 4. STEP ==================
    @Bean
    public Step employeeCsvImportStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      FlatFileItemReader<CsvEmployeeRecord> employeeCsvReader,
                                      EmployeeCsvItemProcessor employeeCsvProcessor,
                                      ItemProcessor<Employee, Employee> employeeOrganizationProcessor,
                                      CompositeItemWriter<Employee> compositeEmployeeWriter,
                                      TaskExecutor taskExecutor) {

        return new StepBuilder("employeeCsvImportStep", jobRepository)
                .<CsvEmployeeRecord, Employee>chunk(10, transactionManager) // Process 50 records per transaction
                .reader(employeeCsvReader)
                .processor(new CompositeItemProcessorBuilder<CsvEmployeeRecord, Employee>()
                        .delegates(employeeCsvProcessor, employeeOrganizationProcessor)
                        .build())
                .writer(compositeEmployeeWriter)
                .listener(employeeCsvProcessor) // IMPORTANT: To trigger the @BeforeStep data loading
                .taskExecutor(taskExecutor)
                .build();
    }

    // ================== 5. JOB ==================
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