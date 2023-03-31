package com.peopleapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimeSpan {

    private List<Count> today;

    private List<Count> currentWeek;

    private List<Count> currentMonth;

    private List<Count> currentYear;

    private int total = 0;
}
