package io.ezto.bg.core.models;

import lombok.Data;

@Data
public class GeneralResponse<T> {
    private int status;
    private T body;
}
