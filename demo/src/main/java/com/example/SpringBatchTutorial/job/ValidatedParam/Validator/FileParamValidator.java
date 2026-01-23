package com.example.SpringBatchTutorial.job.ValidatedParam.Validator;

import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersValidator;
import org.springframework.util.StringUtils;

public class FileParamValidator implements JobParametersValidator {
    // thorws 가 과거에는 JobParametersValidator 로 throws가 걸렸는데 5.0 버전부터는 바뀐것 같음
    @Override
    public void validate(JobParameters parameters) throws InvalidJobParametersException {
        String fileName = parameters.getString("fileName");

        if (!StringUtils.endsWithIgnoreCase(fileName,"csv")){
            throw new InvalidJobParametersException("This is CSV");
        }
    }
}
