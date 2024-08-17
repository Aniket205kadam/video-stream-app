package dev.aniket.playload;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CustomMessage {
    private String message;
    private Boolean success = false;
}
