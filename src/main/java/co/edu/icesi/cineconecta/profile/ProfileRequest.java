package co.edu.icesi.cineconecta.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de entrada para crear o actualizar un perfil de visualización. */
public record ProfileRequest(
        @NotBlank(message = "El nombre del perfil es obligatorio.")
        @Size(max = 60, message = "El nombre del perfil no puede superar 60 caracteres.")
        String name,

        @NotNull(message = "La clasificación máxima de edad es obligatoria.")
        Integer maxAgeRating
) {
}
