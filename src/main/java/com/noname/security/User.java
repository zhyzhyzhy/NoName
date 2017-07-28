package com.noname.security;

public class User {
    private String userName;
    private String password;
    private String[] roles;

    public User(String userName, String password, String[] roles) {
        this.userName = userName;
        this.password = password;
        this.roles = roles;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String[] getRoles() {
        return roles;
    }
}