package com.example.springbatch.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class ScheduledTask {

    private final JobLauncher jobLauncher;

    private final Job simpleScheduleJob;
    private final Job simpleChunkJob;

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

    @Scheduled(cron = "0 * * * * ?")
    public void runSimpleChunkJob(){
        log.info("===========================");
        log.info("simple Schedule Chunk Job 시작");
        log.info("===========================");

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis()) // 중복 실행 방지
                .toJobParameters();
        try{
            jobLauncher.run(simpleChunkJob, jobParameters);
        } catch (Exception e) {
            log.error("simpleScheduleJob 배치를 실패하였습니다. : ", e);
        }

    }

}
