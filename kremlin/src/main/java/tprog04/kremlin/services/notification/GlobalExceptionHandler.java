package tprog04.kremlin.services.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tprog04.kremlin.dto.error.ErrorResponseDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest()
                             .body(new ErrorResponseDTO(
                                "INVALID_ACTION",
                                ex.getMessage())
                             );
    }
}
