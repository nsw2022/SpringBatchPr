package com.example.SpringBatchTutorial.job.JobListener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

@Slf4j
public class JobLoggerListener implements JobExecutionListener {

    private static String BEFORE_MESSAGE = "{} Job is Running";
    private static String AFTER_MESSAGE = "{} Job is Done. (Status : {})";

    // 잡 실행전
    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(BEFORE_MESSAGE, jobExecution.getJobInstance().getJobName());
    }

    // 잡 실행 후
    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info(AFTER_MESSAGE
                , jobExecution.getJobInstance().getJobName()
                , jobExecution.getStatus()
        );

        if (jobExecution.getStatus() == BatchStatus.FAILED){
            log.info("Job is Failed");
        }
    }
}
