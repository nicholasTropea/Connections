package com.nicholasTropea.game.net;

public class RegisterRequest {
    private final String operation;
    private String name;
    private String password;

    public RegisterRequest(String name, String password) {
        this.operation = "register";
        this.name = name;
        this.password = password;
    }

    public String getOperation() { return this.operation; }
    public String getName() { return this.name; }
    public String getPassword() { return this.password; }
}