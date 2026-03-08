package tprog04.kremlin.services.auth;

import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

@Service
public class JwtService {
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private Algorithm algorithm() {
        return Algorithm.HMAC256(jwtSecret);
    }

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpirationMs);

        return JWT.create()
                  .withSubject(userDetails.getUsername())
                  .withIssuedAt(now)
                  .withExpiresAt(exp)
                  .sign(algorithm());
    }

    public String extractUsername(String token) {
        DecodedJWT jwt = JWT.require(algorithm()).build().verify(token);
        return jwt.getSubject();
    }

    public boolean isTokenExpired(String token) {
        DecodedJWT jwt = JWT.require(algorithm()).build().verify(token);
        return jwt.getExpiresAt().before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
