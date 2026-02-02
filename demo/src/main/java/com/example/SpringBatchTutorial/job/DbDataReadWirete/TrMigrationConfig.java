package com.example.SpringBatchTutorial.job.DbDataReadWirete;

import com.example.SpringBatchTutorial.core.domain.accounts.AccountsRepository;
import com.example.SpringBatchTutorial.core.domain.orders.Orders;
import com.example.SpringBatchTutorial.core.domain.orders.OrdersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * desc: 주문 테이블 -> 정산 테이블 데이터 이관
 * run : --spring.batch.job.name=trMigrationJob
 */
@Configuration
@RequiredArgsConstructor
public class TrMigrationConfig {

    private final OrdersRepository ordersRepository;
    private final AccountsRepository accountsRepository;

    // [Spring Batch 5.0] Factory 대신 직접 주입받아 사용하는 필수 구성 요소
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * Job 생성
     * @param trMigrationStep 스프링이 컨텍스트에서 생성된 Step 빈을 자동으로 주입함
     */
    @Bean
    public Job trMigrationJob(Step trMigrationStep) {
        return new JobBuilder("trMigrationJob", jobRepository) // JobBuilder 생성 시 jobRepository 필수
                .start(trMigrationStep) // 직접 메서드 호출()이 아닌 주입된 객체를 사용함
                .build();
    }

    /**
     * Step 생성 (Chunk 지향 방식)
     * @param trOrdersReader @StepScope로 생성된 Reader 빈을 주입받음
     */
    @Bean
    public Step trMigrationStep(ItemReader<Orders> trOrdersReader) {
        return new StepBuilder("trMigrationStep", jobRepository)
                // <Input, Output>chunk(사이즈, 트랜잭션매니저)
                // [변경점] 5.0부터는 트랜잭션 매니저를 chunk 설정 시점에 명시적으로 넘겨야 함 과거에는 List로 받아올수있었음
                .<Orders, Orders>chunk(5)
                .reader(trOrdersReader)
                .writer(new ItemWriter<Orders>() {
                    @Override
                    public void write(Chunk<? extends Orders> chunk) throws Exception {
                        // [변경점] List가 아닌 Chunk 객체로 데이터가 넘어옴 (Iterator 구현체)
                        for (Orders order : chunk) {
                            System.out.println("order = " + order);
                        }
                    }
                })
                .build();
    }

    /**
     * RepositoryItemReader: DB에서 데이터를 읽어오는 리더
     * @StepScope: Step 실행 시점에 빈이 생성되도록 지연 로딩 설정 (파라미터 활용 등에 필수)
     */
    @StepScope
    @Bean
    public RepositoryItemReader<Orders> trOrdersReader(){
        return new RepositoryItemReaderBuilder<Orders>()
                .name("trOrdersReader")
                .repository(ordersRepository)
                .methodName("findAll") // 레포지토리의 메서드 명
                .pageSize(5)           // 페이지 단위로 읽어올 사이즈

                // [arguments 설명]
                // 호출할 메서드(findAll)에 전달할 인자값을 리스트 형태로 설정함.
                // 1. 현재 findAll은 파라미터가 없으므로 Collections.emptyList()를 사용함.
                // 2. 만약 findByStatus(String status)처럼 인자가 필요한 메서드를 사용한다면
                //    .arguments(Arrays.asList("COMPLETED")) 와 같이 값을 넘겨주어야 함.
                .arguments(Collections.emptyList())

                // [sorts 설명]
                // 페이징 기반 리더에서 정렬은 '필수'입니다.
                // 정렬 기준이 없으면 페이지를 넘길 때 데이터 순서가 뒤바뀌어
                // 중복 읽기나 누락이 발생할 수 있으므로, 고유한 값(id 등)으로 반드시 정렬해야 합니다.
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC)) // 정렬 필수
                .build();
    }
}