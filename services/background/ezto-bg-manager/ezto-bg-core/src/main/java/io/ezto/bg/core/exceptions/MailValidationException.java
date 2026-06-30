package io.ezto.bg.core.exceptions;

import io.ezto.bg.core.models.GeneralResponse;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class MailValidationException extends Exception{
    private final GeneralResponse generalResponse;
}
