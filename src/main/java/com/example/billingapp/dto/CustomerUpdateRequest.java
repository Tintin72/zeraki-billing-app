package com.example.billingapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerUpdateRequest {

    @Size(min = 1, message = "Name cannot be blank")
    private String name;

    @Email(message = "Email must be a valid address")
    private String email;

    private String phone;
}
