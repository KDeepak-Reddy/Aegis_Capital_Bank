package com.example.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserProfileDto {
    private String name;
    private String email;
    private String dob;
    private String mobileNo;
    private String panNo;

    // Constructors, getters, setters
    public UserProfileDto() {}

    public UserProfileDto(String name, String email, String dob, String mobileNo, String panNo) {
        this.name = name;
        this.email = email;
        this.dob = dob;
        this.mobileNo = mobileNo;
        this.panNo = panNo;
    }

}