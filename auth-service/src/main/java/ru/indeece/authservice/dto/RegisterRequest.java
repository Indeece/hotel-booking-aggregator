package ru.indeece.authservice.dto;

import lombok.Data;
import ru.indeece.authservice.enums.Role;

@Data
public class RegisterRequest {
    private String username;
    private String password;
}