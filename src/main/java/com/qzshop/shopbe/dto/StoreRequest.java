package com.qzshop.shopbe.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StoreRequest {
    @NotBlank(message = "name is required", groups = CreateStore.class)
    @Pattern(regexp = ".*\\S.*", message = "name must not be blank")
    private String name;
    private String address;
    private String phone;
    private String ownerName;
    private String status;
    private LocalTime openingTime;
    private LocalTime closingTime;
}
