package fi.hsl.suomenlinna_hfp.sbdrive.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public class Token {
    public final Integer expiration;
    public final String token;
    public final String tokenType;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Token(Integer expiration, String token, @JsonProperty("token_type") String tokenType) {
        this.expiration = expiration;
        this.token = token;
        this.tokenType = tokenType;
    }

    public boolean hasExpired() {
        return Instant.ofEpochSecond(expiration).isBefore(Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token1 = (Token) o;
        return Objects.equals(expiration, token1.expiration) &&
                Objects.equals(token, token1.token) &&
                Objects.equals(tokenType, token1.tokenType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expiration, token, tokenType);
    }

    @Override
    public String toString() {
        return "Token{" +
                "expiration=" + expiration +
                ", token='" + token + '\'' +
                ", tokenType='" + tokenType + '\'' +
                '}';
    }
}
