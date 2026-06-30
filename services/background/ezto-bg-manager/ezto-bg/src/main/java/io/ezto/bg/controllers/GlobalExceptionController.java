package io.ezto.bg.controllers;

import io.ezto.bg.core.models.Body;
import io.ezto.bg.core.models.GeneralResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionController {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralResponse> geoException(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(x -> x.getField()+" "+x.getDefaultMessage())
                .collect(Collectors.toList());

        GeneralResponse response = new GeneralResponse<>();
        response.setStatus(400);
        response.setBody(errors);
        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(value = IOException.class)
    public ResponseEntity<GeneralResponse> ioException(IOException exception) {

        Body responseBody = new Body();
        responseBody.setMessage(exception.getMessage());

        GeneralResponse response = new GeneralResponse<>();
        response.setStatus(400);
        response.setBody(responseBody);

        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<GeneralResponse> ioException(Exception exception) {

        Body responseBody = new Body();
        responseBody.setMessage(exception.getMessage());

        GeneralResponse response = new GeneralResponse<>();
        response.setStatus(400);
        response.setBody(responseBody);
        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }
}
