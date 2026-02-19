package com.example.SpringBatchTutorial.job.FileDataReadWriteConfig;

import com.example.SpringBatchTutorial.job.FileDataReadWriteConfig.dto.Player;
import com.example.SpringBatchTutorial.job.FileDataReadWriteConfig.dto.PlayerYears;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * desc: CSV 파일 읽고 쓰기 (Spring Batch 5.0 기준)
 * run: --spring.batch.job.name=fileReadWriteJob
 *
 */
@Configuration
public class FileDataReadWriteConfig {

    // Spring Batch 5.0은 Bean 주입 시 직접 JobRepository와 PlatformTransactionManager를 사용합니다.
    @Bean
    public Job fileReadWriteJob(JobRepository jobRepository, Step fileReadWriteStep) {
        return new JobBuilder("fileReadWriteJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(fileReadWriteStep)
                .build();
    }

    @JobScope
    @Bean
    public Step fileReadWriteStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  ItemReader<Player> playerItemReader,
                                  ItemProcessor<Player, PlayerYears> playerItemProcessor,
                                  ItemWriter<PlayerYears> playerItemWriter) {
        return new StepBuilder("fileReadWriteStep", jobRepository)
                .<Player, PlayerYears>chunk(5) // 트랜잭션 매니저 명시 필수
                .reader(playerItemReader)
                .processor(playerItemProcessor)
                .writer(playerItemWriter)
                .build();
    }

    @StepScope
    @Bean
    public ItemProcessor<Player, PlayerYears> playerItemProcessor() {
        // Player DTO를 PlayerYears  DTO로 변환하는 비즈니스 로직 위치
        return PlayerYears::new;
    }

    @StepScope
    @Bean
    public FlatFileItemReader<Player> playerItemReader() {
        return new FlatFileItemReaderBuilder<Player>()
                .name("playerItemReader")
                /* * .resource(new ClassPathResource("...")): resources 폴더 내의 파일을 읽음
                 * .resource(new FileSystemResource("...")): 서버의 절대경로/상대경로 파일을 읽음
                 */
                //.resource(new FileSystemResource("player.csv"))
                .resource(new ClassPathResource("player.csv"))// resources 에 접근하고 싶다면 객체를 바꿔야함
                .delimited() // 구분자로 나누어진 파일을 읽겠다는 설정 (기본값: 컴마 ,)
                /* * .names(...): CSV의 각 열을 DTO 필드와 매핑 (순서 엄수)
                 * 만약 CSV 헤더 순서가 바뀌면 이 부분도 수정되어야 함
                 */
                .names("ID", "lastName", "firstName", "position", "birthYear", "debutYear") // 필드명 명시 권장(안맞추면 Passing 에러남)
                .targetType(Player.class)// Reflection을 통해 Player 객체 생성 (기본 생성자 필요)
                // Reflection 이란? https://youtu.be/RZB7_6sAtC4?si=IELa1ptOMwrPakLj
                /*
                 Reflection은 복잡할것 없이 역추적을 쉽게하기 위함(역추적이라는 말이 헷갈릴수 있는데 편리함 때문이라고 보면 됨)
                 .targetType(Player.class) 로 작성하지 않았다면
                 // 1. 직접 new로 객체를 생성하고
                 Player player = new Player();

                 // 2. 인덱스나 이름으로 값을 꺼내서 일일이 set 해줘야 함
                 player.setID(fieldSet.readString("ID"));
                 player.setLastName(fieldSet.readString("lastName"));

                 지금처럼 쓰게되면
                 1. 클래스 스캔: Player.class를 보고 "아, 이 객체는 ID, lastName 같은 변수가 있네?"라고 파악.

                 2. 자동 호출: CSV에서 ID라는 컬럼을 읽으면, Player 객체의 setID() 메서드를 자동으로 찾아 호출.

                 3. 타입 변환: CSV의 문자열 "123"을 Player 객체의 Integer ID 필드 타입에 맞춰 자동으로 형변환까지 수행.

                 물론 단점도 있음 setter 의 필드명까지 반드시 일치해야함(대소문자까지), 기본생성자가 반드시 있어야함
                */

                .linesToSkip(1)// 파일의 첫 번째 줄(헤더)은 데이터가 아니므로 무시
                // .strict(true) // [참고] 파일이 없을 때 에러를 낼지 여부 (기본 true)
                .build();
    }

    @StepScope
    @Bean
    public FlatFileItemWriter<PlayerYears> playerItemWriter() {
        return new FlatFileItemWriterBuilder<PlayerYears>()
                .name("playerItemWriter")
                /*
                 * Writer는 JAR 외부에 파일을 생성해야 하므로 FileSystemResource 사용이 일반적
                 */
                .resource(new FileSystemResource("players_output.txt"))
                .delimited() // 기본적으로 .delimited() vs .fixedLength() 로 사용하는데 TSV 같은 공백문자 분류라면 .fixedLength()를 사용한다
                .delimiter("=") // 필드 간 구분자를 "="로 지정
                .names("ID", "lastName", "position", "yearsExperience") // BeanWrapperFieldExtractor 역할
                // .append(true) // [참고] 기존 파일 뒤에 이어쓰고 싶을 때 사용
                .shouldDeleteIfExists(true) // 파일이 이미 있으면 삭제 후 새로 생성
                .build();
    }
}

/*

과거 3.0 대일때의 사용 방법
@Configuration
@RequiredArgsConstructor
public class FileDataReadWriteConfig {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job fileReadWriteJob(Step fileReadWriteStep) {
        return jobBuilderFactory.get("fileReadWriteJob")
                .incrementer(new RunIdIncrementer())
                .start(fileReadWriteStep)
                .build();
    }

    @JobScope
    @Bean
    public Step fileReadWriteStep(ItemReader playerItemReader,
                                  ItemProcessor playerItemProcessor,
                                  ItemWriter playerItemWriter) {
        return stepBuilderFactory.get("fileReadWriteStep")
                .<Player, PlayerYears>chunk(5)
                .reader(playerItemReader)
//                .writer(new ItemWriter() {
//                    @Override
//                    public void write(List items) throws Exception {
//                        items.forEach(System.out::println);
//                    }
//                })
                .processor(playerItemProcessor)
                .writer(playerItemWriter)
                .build();
    }

    @StepScope
    @Bean
    public ItemProcessor<Player, PlayerYears> playerItemProcessor() {
        return new ItemProcessor<Player, PlayerYears>() {
            @Override
            public PlayerYears process(Player item) throws Exception {
                return new PlayerYears(item);
            }
        };
    }

    @StepScope
    @Bean
    public FlatFileItemReader<Player> playerItemReader() {
        return new FlatFileItemReaderBuilder<Player>()
                .name("playerItemReader")
                .resource(new FileSystemResource("Players.csv"))
                .lineTokenizer(new DelimitedLineTokenizer())
                .fieldSetMapper(new PlayerFieldSetMapper())
                .linesToSkip(1)
                .build();
    }

    @StepScope
    @Bean
    public FlatFileItemWriter<PlayerYears> playerItemWriter() {
        BeanWrapperFieldExtractor<PlayerYears> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"ID", "lastName", "position", "yearsExperience"});
        fieldExtractor.afterPropertiesSet();

        DelimitedLineAggregator<PlayerYears> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter("=");
        lineAggregator.setFieldExtractor(fieldExtractor);

        FileSystemResource outputResource = new FileSystemResource("players_output.txt");

        return new FlatFileItemWriterBuilder<PlayerYears>()
                .name("playerItemWriter")
                .resource(outputResource)
                .lineAggregator(lineAggregator)
                .build();
    }
}
*/