package com.example.springbatch.config;

import com.example.springbatch.batch.SimpleScheduleTasklet;
import com.example.springbatch.model.Data;
import com.example.springbatch.repository.DataRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Log4j2
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
    public Step simpleScheduleStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("simpleScheduleStep", jobRepository)
                .tasklet(simpleScheduleTasklet, transactionManager) // .chunk(chunkSize, transactionManager)
                .build();
    }

    /**
     * Chunk 방식
     */
    @Bean
    public Job simpleChunkJob(JobRepository jobRepository, Step simpleChunkStep) { // step 변수명 = 메소드명
        return new JobBuilder("simpleChunkJob", jobRepository)
                .start(simpleChunkStep)
                .build();
    }

    @Bean
    public Step simpleChunkStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("simpleChunkStep", jobRepository)
                .<Data, Data>chunk(10, transactionManager) // <입력형태, 출력형태>chunk(size, transactionManager) : 한 번에 10개의 데이터를 읽고 처리한 후 트랜잭션을 커밋
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
//                .startLimit(1) // 재실행 가능 횟수 : Step의 실행 횟수를 제한(성공/실패 여부와 관계 x)
//                .allowStartIfComplete(true) // 재실행에 포함시키기
//                .faultTolerant()
//                .skipLimit(10) // 데이터를 처리하는 동안 설정된 Exception 이 발생한 경우, 해당 데이터 처리를 건너뛰는 기능
//                .skip(Exception.class) // skip할 Exception, skip된 item만 롤백
//                .noSkip(FileNotFoundException.class) // skip 되지 않는 특정 예외 설정
//                .retryLimit(3) // 재시도 가능 횟수 : Step이 실패했을 때 재시도할 최대 횟수
//                .retry(RuntimeException.class) // 다시 시도할 수 있는 예외, 재시도 후에도 실패하면 트랜잭션 전체가 롤백됨(해당 chunk가 모두 실패)
//                .noRollback(ValidationException.class) // 해당 예외가 발생해도 롤백을 진행하지 않음
                .build();
    }

    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public JpaPagingItemReader<Data> itemReader() {
        return new JpaPagingItemReaderBuilder<Data>()
                .name("data 테이블 JpaPagingItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(10) // pageSize == chunkSize
                .queryString("SELECT d FROM Data d ORDER BY id DESC")
                .build();
    }

    @Bean
    public ItemProcessor<Data, Data> itemProcessor() {
        return data -> {
            data.updateTitle(data.getTitle().toUpperCase());
            return data;
        };
    }

    @Bean
    public JpaItemWriter<Data> itemWriter() {
        JpaItemWriter<Data> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(entityManagerFactory); // 필수로 설정해야할 값이 EntityManager뿐임. JPA 영속성 컨텍스트(EntityManager)를 통해 데이터를 저장
        return jpaItemWriter;
    }

    /**
     * update문을 chunkSize로 줄이는 방법 1
     * * JdbcBatchItemWriter 사용
     */
    private final DataSource dataSource;

    @Bean
    public JdbcBatchItemWriter<Data> itemWriter_jdbc() {
        return new JdbcBatchItemWriterBuilder<Data>()
                .dataSource(dataSource)
                .sql("UPDATE data SET title = :title WHERE id = :id")
                .beanMapped()
                .assertUpdates(true)  // 🔥 영향을 받은 row 수 체크 (예외 발생 가능)
                .build();
    }

    /**
     * update문을 chunkSize로 줄이는 방법 2
     * * 더티 체킹이 아닌 jpql로 작성한 항목을 적용
     * => but, 스케줄러가 돌아갈 때마다 이미 했던 작업도 반복해서 진행한다(where 추가 조건으로 이런 부분을 막야야 할 듯)
     */
    private final DataRepository dataRepository;

    @Bean
    public ItemWriter<Data> itemWriter_repository() {
        return dataList -> {
            for (Data data : dataList) {
                dataRepository.updateTitle(data.getId(), data.getTitle().toUpperCase());
            }
        };
    }
}

/**
 * class형으로 만드는 예시
 */
@Component
class CustomItemWriter implements ItemWriter<Data> {

    @Override
    public void write(Chunk<? extends Data> chunk) throws Exception {
        for (Data item : chunk) {
            System.out.println("Writing item: " + item);  // 로그 출력
        }
    }
}
