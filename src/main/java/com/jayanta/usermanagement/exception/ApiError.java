package com.jayanta.usermanagement.exception;


import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

@Data
@Builder
public class ApiError {
    private HttpStatus status;
    private String message;
    private String code;
    private String field;
    private ZonedDateTime timestamp = ZonedDateTime.now();
}
