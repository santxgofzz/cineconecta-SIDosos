package co.edu.icesi.cineconecta.profile;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Páginas MVC de selección y administración de perfiles de la cuenta autenticada. */
@Controller
@RequestMapping("/profiles")
public class ProfilePageController {
    private static final String REDIRECT_MANAGE = "redirect:/profiles/manage";

    private final ProfileService profileService;
    private final CurrentAccount currentAccount;

    public ProfilePageController(ProfileService profileService, CurrentAccount currentAccount) {
        this.profileService = profileService;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public String select(Model model) {
        long accountId = currentAccount.requireAccountId();
        List<ProfileResponse> allProfiles = profileService.listProfiles(accountId);
        List<ProfileResponse> activeProfiles = allProfiles.stream()
                .filter(ProfileResponse::active)
                .toList();
        model.addAttribute("activeProfiles", activeProfiles);
        model.addAttribute("hasInactiveProfiles", activeProfiles.size() < allProfiles.size());
        return "profile/select";
    }

    @GetMapping("/manage")
    public String manage(Model model) {
        long accountId = currentAccount.requireAccountId();
        model.addAttribute("profiles", profileService.listProfiles(accountId));
        model.addAttribute("ageRatings", profileService.listAgeRatings());
        return "profile/manage";
    }

    @PostMapping("/manage")
    public String create(@RequestParam(defaultValue = "") String name,
                         @RequestParam(required = false) Integer maxAgeRating,
                         RedirectAttributes redirectAttributes) {
        return runProfileMutation(redirectAttributes, () -> {
            ProfileRequest request = validatedRequest(name, maxAgeRating);
            profileService.createProfile(currentAccount.requireAccountId(), request);
            return "Perfil creado correctamente.";
        });
    }

    @PostMapping("/manage/{profileId}/edit")
    public String update(@PathVariable long profileId,
                         @RequestParam(defaultValue = "") String name,
                         @RequestParam(required = false) Integer maxAgeRating,
                         RedirectAttributes redirectAttributes) {
        return runProfileMutation(redirectAttributes, () -> {
            ProfileRequest request = validatedRequest(name, maxAgeRating);
            profileService.updateProfile(currentAccount.requireAccountId(), profileId, request);
            return "Perfil actualizado correctamente.";
        });
    }

    @PostMapping("/manage/{profileId}/deactivate")
    public String deactivate(@PathVariable long profileId, RedirectAttributes redirectAttributes) {
        return runProfileMutation(redirectAttributes, () -> {
            profileService.deactivateProfile(currentAccount.requireAccountId(), profileId);
            return "Perfil desactivado correctamente.";
        });
    }

    @PostMapping("/manage/{profileId}/reactivate")
    public String reactivate(@PathVariable long profileId, RedirectAttributes redirectAttributes) {
        return runProfileMutation(redirectAttributes, () -> {
            profileService.reactivateProfile(currentAccount.requireAccountId(), profileId);
            return "Perfil reactivado correctamente.";
        });
    }

    private String runProfileMutation(RedirectAttributes redirectAttributes, MutationAction action) {
        try {
            String message = action.run();
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (ApiException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_MANAGE;
    }

    private static ProfileRequest validatedRequest(String name, Integer maxAgeRating) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 60) {
            throw new IllegalArgumentException("El nombre del perfil es obligatorio (máximo 60 caracteres).");
        }
        if (maxAgeRating == null) {
            throw new IllegalArgumentException("Selecciona una clasificación máxima de edad.");
        }
        return new ProfileRequest(trimmed, maxAgeRating);
    }

    @FunctionalInterface
    private interface MutationAction {
        String run();
    }
}
