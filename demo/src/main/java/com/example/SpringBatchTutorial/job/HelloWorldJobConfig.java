package com.example.SpringBatchTutorial.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class HelloWorldJobConfig {

    /**
     * [Spring Batch 5.0 변경점 1] Factory 삭제
     * - 과거: JobBuilderFactory, StepBuilderFactory를 사용 (Deprecated/삭제됨)
     * - 현재: JobRepository와 TransactionManager를 직접 주입받아서 Builder에 전달해야 함
     */
    private final JobRepository jobRepository; // Job과 Step의 상태(실행 기록 등)를 DB에 저장/관리하는 저장소
    private final PlatformTransactionManager transactionManager; // 트랜잭션 관리자 (Commit/Rollback 담당)

    /**
     * Job 생성 설정
     * - Job은 배치의 가장 큰 실행 단위입니다.
     */
    @Bean
    public Job helloWorldJob() {
        // [변경점 2] new JobBuilder("이름", jobRepository) 사용
        // Factory.get() 대신 Builder를 직접 생성하며, 두 번째 인자로 jobRepository가 필수입니다.
        return new JobBuilder("helloWorldJob", jobRepository)
                .start(helloWorldStep()) // 첫 번째로 실행할 Step 지정
                .build(); // Job 생성
    }

    /**
     * Step 생성 설정
     * - Step은 Job 내부에서 실제 비즈니스 로직(읽기/처리/쓰기)을 담당하는 단계입니다.
     */
    @Bean
    public Step helloWorldStep() {
        // [변경점 3] new StepBuilder("이름", jobRepository) 사용
        return new StepBuilder("helloWorldStep", jobRepository)

                /* * Tasklet 정의 (단순 작업용)
                 * - 람다(Lambda) 식을 사용하여 코드를 간결하게 작성했습니다.
                 * - (contribution, chunkContext) -> { ... } 구조입니다.
                 */
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Hello, World! Spring Batch 5.0");

                    // Step이 정상적으로 끝났음을 반환 (FINISHED)
                    return RepeatStatus.FINISHED;
                }, transactionManager) // [변경점 4] TransactionManager 필수 전달
                // Spring Batch 5부터는 Step을 만들 때 어떤 트랜잭션 매니저를 쓸지 명시해야 합니다.

                .build(); // Step 생성
    }

}

/*

스프링 3.0.x 대의 버전
@Configuration // 이 클래스가 Spring의 설정(Configuration) 클래스임을 명시
@RequiredArgsConstructor // final이 선언된 필드에 대해 생성자를 자동으로 생성 (의존성 주입)
public class HelloWorldJobConfig {

    // Job과 Step을 쉽게 생성할 수 있도록 도와주는 빌더 팩토리 (Spring Batch 5.0 이전 방식)
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;


      //Job 생성 설정
      //- Job은 배치의 가장 큰 실행 단위입니다.

    @Bean
    public Job helloWorldJob() {
        return jobBuilderFactory.get("helloWorldJob") // "helloWorldJob"이라는 이름으로 Job 생성
                // RunIdIncrementer: Job 실행 시마다 파라미터 ID를 증가시켜, 같은 Job을 여러 번 재실행할 수 있게 함
                .incrementer(new RunIdIncrementer())
                .start(helloWorldStep()) // Job 시작 시 실행할 첫 번째 Step 지정
                .build(); // Job 빌드 및 반환
    }


     //Step 생성 설정
    // - Step은 Job 내부에서 실질적인 처리를 담당하는 단계입니다.
    //  - @JobScope: Job이 실행될 때 이 Bean이 생성되도록 설정 (Late Binding)

    @JobScope
    @Bean
    public Step helloWorldStep() {
        return stepBuilderFactory.get("helloWorldStep") // "helloWorldStep"이라는 이름으로 Step 생성
                .tasklet(helloWorldTasklet()) // 이 Step에서 수행할 기능(Tasklet)을 지정
                .build(); // Step 빌드 및 반환
    }

    //
     // Tasklet 생성 설정
     // - Tasklet은 Step 안에서 단일 작업을 수행하는 로직입니다. (단순 작업용)
     // - @StepScope: Step이 실행될 때 이 Bean이 생성되도록 설정
     ///
    @StepScope
    @Bean
    public Tasklet helloWorldTasklet() {
        // 익명 클래스로 Tasklet 구현
        return new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                // 비즈니스 로직 작성 구간
                System.out.println("Hello World Spring Batch");

                // 이 Tasklet의 처리가 끝났음을 반환 (FINISHED: 종료, CONTINUABLE: 다시 실행)
                return RepeatStatus.FINISHED;
            }
        };
    }
}

* */