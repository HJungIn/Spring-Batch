package com.example.springbatch.config;

import com.example.springbatch.batch.SimpleScheduleTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class SimpleBatchConfig extends DefaultBatchConfiguration {

    private final SimpleScheduleTasklet simpleScheduleTasklet;


    @Bean
    public Job simpleScheduleJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) { // 숨겨진 자동 설정 명시적으로 변경 : 각 Step과 Job이 동일한 JobRepository를 공유하도록 보장하기 위해서
        return new JobBuilder("simpleScheduleJob", jobRepository)
                .start(simpleScheduleStep(jobRepository, transactionManager))
                .build();
    }


    @Bean
    public Step simpleScheduleStep(JobRepository jobRepository, PlatformTransactionManager transactionManager){
        return new StepBuilder("simpleScheduleStep", jobRepository)
                .tasklet(simpleScheduleTasklet, transactionManager) // .chunk(chunkSize, transactionManager)
                .build();
    }

}
