package com.narek.jobportal.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class WordCountValidator implements ConstraintValidator<WordCount, String> {

    private int max;

    @Override
    public void initialize(WordCount constraintAnnotation) {
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String[] words = value.trim().split("\\s+");
        return words.length <= max;
    }
}