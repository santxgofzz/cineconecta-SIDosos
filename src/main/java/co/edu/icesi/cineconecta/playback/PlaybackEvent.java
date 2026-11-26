package co.edu.icesi.cineconecta.playback;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Evento del reproductor simulado.
 * <ul>
 *   <li>WATCH: se reprodujeron {@code seconds} segundos (cuentan como vistos y avanzan la posición).</li>
 *   <li>SEEK: salto a {@code positionSeconds} (cambia la posición, NO suma tiempo visto).</li>
 *   <li>PAUSE: cierra la sesión como "paused"; reanudar crea una nueva sesión.</li>
 *   <li>FINISH: cierra la sesión como "finished" y marca la unidad completada.</li>
 * </ul>
 *
 * @param seq número creciente por sesión, asignado por el cliente. Un evento
 *            con seq ya aplicado es un reintento y se ignora (idempotencia).
 */
public record PlaybackEvent(@NotNull @Min(1) Integer seq, @NotNull Type type, Integer seconds,
                            Integer positionSeconds) {
    public enum Type { WATCH, SEEK, PAUSE, FINISH }
}
