package com.example.SpringBatchTutorial.job.DbDataReadWirete;

import com.example.SpringBatchTutorial.core.domain.accounts.Accounts;
import com.example.SpringBatchTutorial.core.domain.accounts.AccountsRepository;
import com.example.SpringBatchTutorial.core.domain.orders.Orders;
import com.example.SpringBatchTutorial.core.domain.orders.OrdersRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.*;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.RepositoryItemWriter;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemWriterBuilder;
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
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * Job 생성
     */
    @Bean
    public Job trMigrationJob(Step trMigrationStep) {
        return new JobBuilder("trMigrationJob", jobRepository)
                .start(trMigrationStep)
                .build();
    }

    /**
     * Step 생성
     * @param trOrdersReader 주입받은 Reader 빈
     * @param trOrderProcessor 주입받은 Processor 빈
     * @param trOrderWriter 주입받은 Writer 빈
     */
    @Bean
    public Step trMigrationStep(ItemReader<Orders> trOrdersReader,
                                ItemProcessor<Orders, Accounts> trOrderProcessor,
                                ItemWriter<Accounts> trOrderWriter) {
        return new StepBuilder("trMigrationStep", jobRepository)
                // [Spring Batch 5.0] 반드시 transactionManager를 두 번째 인자로 넘겨야 합니다.
                // <읽기타입, 쓰기타입>chunk(사이즈, 매니저)
                .<Orders, Accounts>chunk(5)
                .reader(trOrdersReader)
                //      .writer(new ItemWriter<Orders>() {
                //                    @Override
                //                    public void write(Chunk<? extends Orders> chunk) throws Exception {
                //                        // [변경점] List가 아닌 Chunk 객체로 데이터가 넘어옴 (Iterator 구현체)
                //                        for (Orders order : chunk) {
                //                            System.out.println("order = " + order);
                //                        }
                //                    }
                //                })
                .processor(trOrderProcessor)
                .writer(trOrderWriter)
                .build();
    }

    /**
     * ItemProcessor: Orders 엔티티를 Accounts 엔티티로 변환
     */
    @StepScope
    @Bean
    public ItemProcessor<Orders, Accounts> trOrderProcessor() {
        return item -> new Accounts(item);
    }

    /**
     * RepositoryItemWriter: 변환된 Accounts 엔티티를 DB에 저장
     */
    @StepScope
    @Bean
    public RepositoryItemWriter<Accounts> trOrderWriter() {
        return new RepositoryItemWriterBuilder<Accounts>()
                .repository(accountsRepository)
                .methodName("save") // JpaRepository의 save 메서드 사용
                .build();
    }

    /**
     * RepositoryItemReader: DB에서 Orders 데이터를 페이징하여 읽기
     * StepScope: Step 실행 시점에 빈이 생성되도록 지연 로딩 설정 (파라미터 활용 등에 필수)
     */
    @StepScope
    @Bean
    public RepositoryItemReader<Orders> trOrdersReader() {
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