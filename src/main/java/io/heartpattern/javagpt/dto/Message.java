package io.heartpattern.javagpt.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Message {
    private Role role;
    private String content;
    public enum Role {
        USER,
        SYSTEM,
        ASSISTANT;

        @JsonValue
        public String toLowerCase() {
            return toString().toLowerCase();
        }
    }
}
