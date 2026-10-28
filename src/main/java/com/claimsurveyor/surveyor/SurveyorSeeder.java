package com.claimsurveyor.surveyor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Adds a few surveyors on first start so the assignment feature works out of the box. */
@Component
public class SurveyorSeeder implements ApplicationRunner {

    private final SurveyorRepository repository;

    public SurveyorSeeder(SurveyorRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        repository.save(new Surveyor("Meera Iyer", "Thane"));
        repository.save(new Surveyor("Priya Nair", "Pune"));
        repository.save(new Surveyor("Rahul Verma", "Mumbai"));
        repository.save(new Surveyor("Sanjay Kulkarni", "Nashik"));
    }
}
