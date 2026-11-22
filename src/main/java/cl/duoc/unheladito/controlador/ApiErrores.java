package cl.duoc.unheladito.controlador;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrores {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> estado(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode())
                .body(Map.of("message", error.getReason() == null ? "Solicitud rechazada." : error.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException error) {
        var campo = error.getBindingResult().getFieldError();
        String texto = campo == null ? "Revisa los datos enviados." : "Revisa el campo " + campo.getField() + ".";
        return ResponseEntity.badRequest().body(Map.of("message", texto));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> formato() {
        return ResponseEntity.badRequest().body(Map.of("message", "Los datos enviados tienen un formato inválido."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflicto() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Los datos entran en conflicto con un registro existente."));
    }
}
