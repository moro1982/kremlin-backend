package tprog04.kremlin.dto.auth;

import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;

    public AuthResponse(String token) {
        this.accessToken = token;
    }
}
