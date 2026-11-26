package co.edu.icesi.cineconecta.playback;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Páginas de la interfaz simulada. Solo entregan la vista: los datos se
 * obtienen por la API, que aplica todas las validaciones de servidor.
 */
@Controller
public class PlaybackPageController {

    @GetMapping("/profiles/{profileId}/player")
    public String player(@PathVariable long profileId, @RequestParam String contentId,
                         @RequestParam(required = false) String episodeId, Model model) {
        model.addAttribute("profileId", profileId);
        model.addAttribute("contentId", contentId);
        model.addAttribute("episodeId", episodeId);
        return "playback/player";
    }

    /** Mi lista, Continuar viendo e historial del perfil. */
    @GetMapping("/profiles/{profileId}/activity")
    public String activity(@PathVariable long profileId, Model model) {
        model.addAttribute("profileId", profileId);
        return "playback/activity";
    }
}
