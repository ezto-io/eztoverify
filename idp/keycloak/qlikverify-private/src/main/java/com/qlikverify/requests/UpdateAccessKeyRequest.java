package com.qlikverify.requests;

import lombok.Data;
import lombok.Getter;

import javax.validation.constraints.NotEmpty;
import java.util.Map;

@Data
@Getter
public class UpdateAccessKeyRequest {
    @NotEmpty
    private String name;
    @NotEmpty
    private boolean enabled;
    @NotEmpty
    private Map<String, Boolean> permissions;
    @NotEmpty
    private long expiryDate;

    @NotEmpty
    private boolean sandboxMode;

}
