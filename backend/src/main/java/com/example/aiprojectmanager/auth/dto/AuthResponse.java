package com.example.aiprojectmanager.auth.dto;

public record AuthResponse(
    String token,
    UserDto user
) {}
