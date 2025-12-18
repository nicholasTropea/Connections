package com.nicholasTropea.game.net;

public class RegisterResponse {
    private String status;
    private String message;

    public RegisterResponse(String status) {
        this.status = status;

        switch (status) {
            case "ok":
                this.message = "Registrazione andata a buon fine.";
                break;
            case "fail":
                this.message = "Errore durante la registrazione.";
                break;
            default:
                this.message = "Stato sconosciuto.";
        }
    }

    public String getStatus() { return this.status; }
    public String getMessage() { return this.message; }
}