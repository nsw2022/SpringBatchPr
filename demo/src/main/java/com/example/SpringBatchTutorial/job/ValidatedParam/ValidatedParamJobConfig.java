package com.example.SpringBatchTutorial.job.ValidatedParam;

import com.example.SpringBatchTutorial.job.ValidatedParam.Validator.FileParamValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.CompositeJobParametersValidator;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;

/**
 * desc: 파일 이름 파라미터 전달 그리고 검증
 * run: --spring.batch.job.name=validateParamJob -fileName=test.csv
 */
@Configuration
@RequiredArgsConstructor
public class ValidatedParamJobConfig {

    /*
     * [Spring Batch 5.0 변경점]
     * JobBuilderFactory, StepBuilderFactory가 Deprecated(삭제)되었습니다.
     * 대신 JobRepository와 PlatformTransactionManager를 직접 주입받아 사용합니다.
     */
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    @Bean
    public Job validateParamJob() {
        /*
         * [변경] new JobBuilder("Job이름", jobRepository) 형태로 생성합니다.
         */
        return new JobBuilder("validateParamJob", jobRepository)
                .incrementer(new RunIdIncrementer()) // 실행할 때마다 JobParameter에 run.id를 증가시켜 재실행 가능하게 함
                .validator(new FileParamValidator()) // 검증 로직이 필요하면 여기에 validator를 추가합니다.
                .start(validateParamJobStep()) // 첫 번째 Step 시작
                .build();
    }

    private CompositeJobParametersValidator multipleValidator(){
        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
        validator.setValidators(Arrays.asList(new FileParamValidator())); // 여러개의 검증 클래스를 변수로 할당가능
        return  validator;
    }

    @Bean
    @JobScope // Job 실행 시점에 Bean이 생성되도록 설정
    public Step validateParamJobStep() {
        /*
         * [변경] new StepBuilder("Step이름", jobRepository) 형태로 생성합니다.
         */
        return new StepBuilder("validateParamStep", jobRepository)
                /*
                 * [변경] tasklet이나 chunk를 설정할 때 TransactionManager를 반드시 인자로 넘겨야 합니다.
                 */
                .tasklet(validateParamTasklet(), platformTransactionManager)
                .build();
    }

    @Bean
    @StepScope // Step 실행 시점에 Bean이 생성되도록 설정
    public Tasklet validateParamTasklet() {
        return (contribution, chunkContext) -> {
            // 비즈니스 로직 영역
            System.out.println("validateParamTasklet 호출됐나요~");

            // Tasklet 종료 상태 반환
            return RepeatStatus.FINISHED;
        };
    }
}
/*
  [Spring Boot 2.x ~ Batch 4.x 버전 기록용]
  당시 특징: JobBuilderFactory와 StepBuilderFactory를 사용하여 간편하게 생성.
  현재(5.0+)는 삭제된 방식임.

@Configuration
@RequiredArgsConstructor
public class ValidatedParamJobConfig {


      [과거 방식의 핵심]
      이 당시에는 @EnableBatchProcessing이 선언되어 있으면
      스프링이 자동으로 JobBuilderFactory, StepBuilderFactory를 빈으로 등록해줬음.
      -> 개발자가 JobRepository나 TransactionManager를 신경 쓸 필요 없이 Factory만 주입받으면 됐음.

      [코드가 왜 방식을 바꾼이유에 대한 고찰]
      과거에는 Factory 방식이 유행했다면 요즘은 Builder 방식으로 전환하려는것 같음.
      추가로 PlatformTransactionManager 을 과거에는 써서 DB에 접근을 하였는데 이 방식이 DB를 하나를 바라보게끔 되어있었다고함
      그래서 Step 마다 트랜잭션을 사용하려면 설정이 까다로웠다고(Bean을 재정의 하거나 Factory를 커스텀)
      현재는 Step을 만들때 트랜잭션을 인자로 직접 넘겨주기에 Step A는 metaTransactionManager Step B 는 serviceTransactionManager
      로 설정하는것으로 쉬워졌다고함
      https://github.com/spring-projects/spring-batch/wiki/Spring-Batch-5.0-Migration-Guide
      공식 깃헛 사이트에서 Transaction manager bean exposure/configuration 을 컨트롤 F 해서 보면 상세히 나와있음

      아래 슬로건을 미는 느낌인거같음
      개발자가 코드를 짤 때 귀찮더라도, '누가(Repository)', '어떻게(TransactionManager)' 일하는지 명시적으로 적어라. 그래야 나중에 사고가 덜 난다

      5.0있는데 왜 2.x ~ 4.x 를 기록함 이라할수있는데 프로젝트를 뛰다보면 3.0버전대가 너무 많음 5.0으로 신규로 구현할 수도 있으니
      과거와 현재 방식을 기록하는거에 초점 두는게 좋다생각함

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job validateParamJob() {

          [Job 생성]
          factory.get("이름")을 호출하면 내부적으로 JobRepository가 자동 주입된 빌더가 반환됨.
          -> 5.0부터는 new JobBuilder("이름", jobRepository)로 직접 주입해야 함.

        return jobBuilderFactory.get("validateParamJob")
                .incrementer(new RunIdIncrementer())
                //.validator(new FileParamValidator()) // 여기서 정의해줘도 되지만 재활용성을 위해 별도의 클래스로 제작 job.ValidatedParam.FileParamValidator
                .validator(multipleValidator)
                .start(validateParamJobStep())
                .build();
    }

    private CompositeJobParametersValidator multipleValidator(){
        CompositeJobParametersValidator validator = new CompositeJobParametersValidator();
        validator.setValidators(Arrays.asList(new FileParamValidator()));
         return  validator;
    }

    @JobScope
    @Bean
    public Step validateParamJobStep() {

          [Step 생성]
          factory.get("이름") 사용.
          특징: .tasklet() 설정 시 TransactionManager를 넣지 않아도
          Factory가 내부적으로 가지고 있는 기본 TransactionManager를 사용함.
          -> 이 '암시적(Implicit) 구성'이 편하긴 했으나, 명확성이 떨어져 5.0에서 제거됨.

        return stepBuilderFactory.get("validateParamStep")
                .tasklet(validateParamTasklet())  트랜잭션 매니저 생략 가능했음
                .build();
    }

    @StepScope
    @Bean
    public Tasklet validateParamTasklet(@Value("#{jobParameters['fileName']} String fileName) {
          //System.out.println(fileName);
          // 여기서 파일명을 받아 작업할 수 있지만
          // Tasklet 까지 오기 전 Job이 실행할 때 검증할 수 있도록 Validator 를 제공을 함 위쪽의 validateParamJob 메서드로 이동

          [Tasklet 구현]
          익명 클래스 방식으로 구현.
          단순 출력 후 FINISHED를 반환하여 스텝을 종료함.

        return (contribution, chunkContext) -> {
            System.out.println("validateParamTasklet");
            return RepeatStatus.FINISHED;
        };
    }
}
*/