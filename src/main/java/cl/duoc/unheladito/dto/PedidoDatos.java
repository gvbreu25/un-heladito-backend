package cl.duoc.unheladito.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record PedidoDatos(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 160) String street,
        @NotBlank @Size(max = 60) String region,
        @NotBlank @Size(max = 60) String commune,
        @NotBlank @Pattern(regexp = "Domicilio estándar|Retiro en tienda") String delivery,
        @Size(max = 300) String notes,
        @NotEmpty @Size(max = 50) List<@Valid @NotNull Item> items
) {
    public record Item(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{1,24}") String productId,
            @Min(1) @Max(1000) int quantity
    ) {}
}
