# Spring Batch

##### <버전>
* java : 17
* spring boot : 3.4.2

### Spring batch
* 배치 프로세싱 == **일괄처리**
* 대량의 데이터를 처리하고 관리하기 위한 엔터프라이즈급 애플리케이션을 구축하기 위한 경량 배치 프레임워크
* **읽고 → 가공하고 → 저장하는 프로세스를 실행**하는 방식인 Spring 기반의 배치 처리 프레임워크
* 제공하는 기능
    * **로깅/추적**
    * **트랜잭션 관리**
    * **작업 처리 통계**
    * **작업 재시작, 건너뛰기**
    * **리소스 관리 등 대용량 레코드 처리에 필수적인 기능**
* 배치가 실패하여 작업을 재시작 시 처음부터가 아닌 실패한 지점부터 실행을 한다.
* 중복 실행을 막기 위해 성공한 이력이 있는 Batch는 동일한 Parameter로 실행 시 Excpetino이 발생한다.

#### "일괄 처리"의 사용 예시
* 대용량의 비지니스 데이터를 복잡한 작업으로 처리해야하는 경우
* 특정한 시점에 스케줄러를 통해 자동화된 작업이 필요한 경우
* 대용량 데이터의 포맷을 변경, 유효성 검사 등의 작업을 트랜잭션 안에서 처리 후 기록해야하는 경우

#### Spring Batch vs Quartz? 또는 Scheduler?
> Spring Batch는 Scheduler가 아니기에 비교 대상이 아니다.
* Spring Batch는 Batch Job을 관리하지만 Job을 구동하거나 실행시키는 기능은 지원하고 있지 않다. 
* Spring에서 Batch Job을 실행시키기 위해서는 Quartz, Scheduler, Jenkins등 전용 Scheduler를 사용하여야 한다.

#### Spring Batch 원칙 및 가이드
* **배치와 서비스에 영향**을 최소화할 수 있도록 구조와 환경에 맞게 디자인하기
* 배치 어플리케이션 내에서 가능한한 복잡한 로직은 피하고 단순하게 설계하기
* 데이터 처리하는 곳과 데이터의 저장소는 물리적으로 가능한한 가까운 곳에 위치하게 하기
* I/O 등의 시스템 리소스의 사용을 최소화하고 **최대한 많은 데이터를 메모리 위에서 처리**하기
* 처리 시간이 많이 걸리는 작업을 시작하기 전에 메모리 재할당에 소모되는 시간을 피하기 위해 충분한 메모리를 할당하기
* 데이터 무결성을 위해서 적절한 **검사 및 기록**하는 코드를 추가하기

<br/><br/>

### Spring Batch Job 구성 및 개념
![img.png](img.png)
##### Job
* 배치처리 과정을 하나의 단위로 만들어 놓은 객체
* 배치처리 과정에 있어 전체 계층 최상단에 위치함.
* 하나의 애플리케이션에서 2개의 Batch Job은 지원하지 않는다.
  * 여러개의 Job 이 있을 경우, 애플리케이션 시작시에 실행하고 싶은 Job 을 application.yaml or properties 에 spring.batch.job.name 에 JobName 을 작성해주도록 한다.

##### JobInstance
* Job의 실행 단위
* Job을 실행 시 = 1개의 JobInstance가 생성
    * ex) 1월 1일 실행, 1월 2일 실행 시 각각의 JobInstance가 생성되며 1월 1일 실행한 JobInstance가 실패하여 다시 실행을 시키더라도 이 JobInstance는 1월 1일에 대한 데이터만 처리하게 된다.

##### JobParameters
* JonInstance의 구별법 : JobParameters 객체로 구분
    * JobParameters : JobInstance 구별 외에도 JobInstacne에 전달되는 매개변수 역할도 하고 있다.
    * JobParameters의 형식 : String, Double, Long, Date

##### JobExecution
* JobInstance에 대한 실행 시도에 대한 객체
    * ex) 1월 1일에 실행한 JobInstacne가 실패하여 재실행을 하여도 동일한 JobInstance를 실행시키지만, 기존에 실행 시킨 것과 재실행 시킨 것에 대한 JobExecution은 개별로 생긴다.
* JobExecution은 JobInstance 실행에 대한 상태,시작시간, 종료시간, 생성시간 등의 정보를 갖고 있다.

##### Step
* Job의 배치처리를 정의하고 순차적인 단계를 캡슐화한 것
* Job은 최소한 1개 이상의 Step을 가져야 하며 Job의 실제 일괄 처리를 제어하는 모든 정보가 들어있다.

##### StepExecution
* Step 실행 시도에 대한 객체
* 하지만 Job이 여러개의 Step으로 구성되어 있을 경우 이전 단계의 Step이 실패하게 되면 다음 단계가 실행되지 않음으로 실패 이후 StepExecution은 생성되지 않는다.
* JobExecution과 동일하게 실제 시작이 될 때만 생성된다.
* JobExecution에 저장되는 정보 외에 read 수, write 수, commit 수, skip 수 등의 정보들도 저장된다.

##### ExecutionContext
* Job에서 데이터를 공유 할 수 있는 데이터 저장소
* Spring Batch에서 제공하는 ExecutionContext : JobExecutionContext, StepExecutionContext (차이 : 저장되는 타이밍이 다름)
    * JobExecutionContext : Commit 시점에 저장
    * StepExecutionContext : 실행 사이에 저장
* ExecutionContext를 통해 Step간 Data 공유가 가능하며 Job 실패시 ExecutionContext를 통한 마지막 실행 값을 재구성할 수 있다.

##### JobRepository
* 모든 배치 처리 정보를 담고있는 매커니즘
* Job이 실행 -> JobRepository에 JobExecution과 StepExecution을 생성 -> JobRepository에서 Execution 정보들을 저장하고 조회하며 사용

##### JobLauncher
* Job과 JobParameters를 사용하여 Job을 실행하는 객체

##### ItemReader
* Step에서 Item을 읽어오는 인터페이스
* ItemReader에 대한 다양한 인터페이스가 존재하여 다양한 방법으로 Item을 읽어올 수 있다.

##### ItemWriter
* 처리 된 Data를 Writer 할 때 사용
* 처리 결과물에 따라 Insert가 될 수도 Update가 될 수도 Queue를 사용한다면 Send가 될 수도 있다.
* Read와 동일하게 다양한 인터페이스가 존재한다.
* Writer는 기본적으로 Item을 Chunk로 묶어 처리하고 있다.

##### ItemProcessor
* Reader에서 읽어온 Item을 데이터를 처리하는 역할
* Processor는 배치를 처리하는데 필수 요소는 아니며 Reader, Writer, Processor 처리를 분리하여 각각의 역할을 명확하게 구분한다.


### Spring Batch 사용하기
![img_1.png](img_1.png)
* Job : 1개 이상의 Step들의 모음이며, 순차적인 Step을 수행하며 Batch를 수행한다.
* Step 방식 : Tasklet 처리 방식, Chunk 지향 처리 방식

##### Tasklet 기반 처리 (Tasklet-Oriented Processing)
* 한 번에 하나의 작업을 실행하는 방식
* 단순한 작업(예: 로그 출력, 파일 이동, DB 초기화 등)에 적합함.
* 개발자가 직접 Tasklet을 구현해야 함.
* 적용예제
    0. 의존성 추가 : ```implementation 'org.springframework.boot:spring-boot-starter-batch'```
    1. Tasklet 생성
        ```java
        @Component
        public class SimpleTasklet implements Tasklet {
        
            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                System.out.println("Tasklet 방식으로 Step 실행!");
                return RepeatStatus.FINISHED; // 한 번 실행 후 종료
            }
        }
        ```
        * ```RepeatStatus.FINISHED``` 반환 : 한 번만 실행하고 종료
        * ```RepeatStatus.CONTINUABLE``` 반환 : 하면 계속 실행
    2. Tasklet를 Step에 등록 
        ```java
        @Configuration
        @RequiredArgsConstructor
        public class SimpleBatchConfig extends DefaultBatchConfiguration{
        
            private final SimpleTasklet simpleTasklet;
        
        
            @Bean
            public Job simpleJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
                return new JobBuilder("simpleScheduleJob", jobRepository)
                        .start(simpleStep(jobRepository, transactionManager))
                        .build();
            }
        
        
            @Bean
            public Step simpleStep(JobRepository jobRepository, PlatformTransactionManager transactionManager){
                return new StepBuilder("simpleStep", jobRepository)
                        .tasklet(simpleTasklet, transactionManager) // .chunk(chunkSize, transactionManager)
                        .build();
            }
        }
        ```
    3. @Scheduler를 통한 실행
        ```java
        @Log4j2
        @Component
        @RequiredArgsConstructor
        public class ScheduledTask {
        
            private final JobLauncher jobLauncher;
        
            private final Job simpleScheduleJob;
        
            @Scheduled(cron = "*/30 * * * * ?")
            public void runSimpleJob(){
               log.info("===========================");
               log.info("simple Schedule Job 시작");
               log.info("===========================");
        
                JobParameters jobParameters = new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis()) // 중복 실행 방지
                        .toJobParameters();
                try{
                    jobLauncher.run(simpleScheduleJob, jobParameters);
                } catch (Exception e) {
                    log.error("simpleScheduleJob 배치를 실패하였습니다. : ", e);
                }
        
            }
        
        }

        ```
    4. 결과
        ```
        2025-02-17T09:09:14.626+09:00  INFO 21888 --- [spring-batch] [           main] c.e.springbatch.SpringBatchApplication   : Started SpringBatchApplication in 4.174 seconds (process running for 4.675)
        2025-02-17T09:09:30.014+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] c.e.springbatch.schedule.ScheduledTask   : ===========================
        2025-02-17T09:09:30.016+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] c.e.springbatch.schedule.ScheduledTask   : simple Schedule Job 시작
        2025-02-17T09:09:30.016+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] c.e.springbatch.schedule.ScheduledTask   : ===========================
        2025-02-17T09:09:30.105+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] o.s.b.c.l.s.TaskExecutorJobLauncher      : Job: [SimpleJob: [name=simpleScheduleJob]] launched with the following parameters: [{'time':'{value=1739750970016, type=class java.lang.Long, identifying=true}'}]
        2025-02-17T09:09:30.145+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] o.s.batch.core.job.SimpleStepHandler     : Executing step: [simpleScheduleStep]
        10
        9
        8
        7
        6
        5
        4
        3
        2
        1
        2025-02-17T09:09:40.270+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] o.s.batch.core.step.AbstractStep         : Step: [simpleScheduleStep] executed in 10s123ms
        2025-02-17T09:09:40.313+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] o.s.b.c.l.s.TaskExecutorJobLauncher      : Job: [SimpleJob: [name=simpleScheduleJob]] completed with the following parameters: [{'time':'{value=1739750970016, type=class java.lang.Long, identifying=true}'}] and the following status: [COMPLETED] in 10s187ms
        2025-02-17T09:10:00.006+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] c.e.springbatch.schedule.ScheduledTask   : ===========================
        2025-02-17T09:10:00.006+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] c.e.springbatch.schedule.ScheduledTask   : simple Schedule Job 시작
        2025-02-17T09:10:00.006+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] c.e.springbatch.schedule.ScheduledTask   : ===========================
        2025-02-17T09:10:00.039+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] o.s.b.c.l.s.TaskExecutorJobLauncher      : Job: [SimpleJob: [name=simpleScheduleJob]] launched with the following parameters: [{'time':'{value=1739751000006, type=class java.lang.Long, identifying=true}'}]
        2025-02-17T09:10:00.071+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] o.s.batch.core.job.SimpleStepHandler     : Executing step: [simpleScheduleStep]
        10
        9
        8
        7
        6
        5
        4
        3
        2
        1
        2025-02-17T09:10:10.231+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] o.s.batch.core.step.AbstractStep         : Step: [simpleScheduleStep] executed in 10s160ms
        2025-02-17T09:10:10.283+09:00  INFO 21888 --- [spring-batch] [   scheduling-1] o.s.b.c.l.s.TaskExecutorJobLauncher      : Job: [SimpleJob: [name=simpleScheduleJob]] completed with the following parameters: [{'time':'{value=1739751000006, type=class java.lang.Long, identifying=true}'}] and the following status: [COMPLETED] in 10s228ms
        ```






##### 출처
* 이론 : https://khj93.tistory.com/entry/Spring-Batch%EB%9E%80-%EC%9D%B4%ED%95%B4%ED%95%98%EA%B3%A0-%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0
* 이론 : https://azderica.github.io/01-spring-batch/