package com.nakivo.job_processing.common.helper;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NonEmptyJsonNodeValidator
        implements ConstraintValidator<NonEmptyJsonNode, JsonNode> {

    @Override
    public boolean isValid(JsonNode value, ConstraintValidatorContext context) {
        return value != null && !value.isEmpty();
    }
}
