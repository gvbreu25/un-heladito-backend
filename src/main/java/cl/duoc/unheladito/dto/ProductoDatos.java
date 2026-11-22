package cl.duoc.unheladito.dto;

import jakarta.validation.constraints.*;

public record ProductoDatos(
        @Pattern(regexp = "[A-Za-z0-9-]{1,24}") String id,
        @NotBlank @Size(max = 100) String name,
        @NotNull @Positive Long categoryId,
        @NotNull @Min(1) @Max(1_000_000) Integer price,
        @NotNull @Min(0) @Max(100_000) Integer stock,
        @NotBlank @Size(max = 40) String format,
        @Size(max = 100) String allergens,
        @NotBlank @Size(max = 500) String description,
        @Size(max = 500) @Pattern(regexp = "^$|^/images/[a-zA-Z0-9._-]+$|^https://[^\\s]+$") String image,
        boolean offer
) {}
