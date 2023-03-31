package com.peopleapp.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

public class EnumValidatorImpl implements ConstraintValidator<EnumValidator, String> {

    List<String> valueList = null;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!valueList.contains(value.toUpperCase())) {
            context.buildConstraintViolationWithTemplate("value should be one of" + valueList)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    @Override
    public void initialize(EnumValidator constraintAnnotation) {
        valueList = new ArrayList<>();
        Class<? extends Enum<?>> enumClass = constraintAnnotation.enumClazz();

        @SuppressWarnings("rawtypes")
        Enum[] enumValArr = enumClass.getEnumConstants();

        for (@SuppressWarnings("rawtypes")
                Enum enumVal : enumValArr) {
            valueList.add(enumVal.toString().toUpperCase());
        }

    }

}