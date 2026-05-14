package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UserDto {
    private Long id;
    private String name;
    private String nickname;
    private String employeeId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;
    private String profileImageUrl;
    private String bio;
    private List<String> hobbies;
    private String communicationStyle;
    private String companyBelief;

    private String email;
    private String password;
    private String role;
    private String department;
    private String jobTitle;
    private String mbti;

    // ソフトデリート情報
    private boolean active;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate retiredAt;

    private LocalDate joinedAt;
}
