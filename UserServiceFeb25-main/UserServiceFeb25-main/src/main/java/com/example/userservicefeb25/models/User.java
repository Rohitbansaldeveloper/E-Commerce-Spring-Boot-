package com.example.userservicefeb25.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity(name = "users")
public class User extends BaseModel {
    private String name;
    private String email;
    private String password;

    // Null is treated as verified for legacy users created before OTP was introduced.
    @jakarta.persistence.Column(name = "email_verified")
    private Boolean emailVerified = false;

    @ManyToMany
    private List<Role> roles;
}

/*

  1         M
User ----- Role => M:M
  M         1


 */
