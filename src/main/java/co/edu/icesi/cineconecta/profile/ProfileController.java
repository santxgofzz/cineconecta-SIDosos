package co.edu.icesi.cineconecta.profile;

import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints REST para consultar y administrar perfiles de la cuenta autenticada. */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    private final ProfileService profiles;
    private final CurrentAccount currentAccount;

    public ProfileController(ProfileService profiles, CurrentAccount currentAccount) {
        this.profiles = profiles;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public List<ProfileResponse> list() {
        return profiles.listProfiles(currentAccount.requireAccountId());
    }

    @GetMapping("/{profileId}")
    public ProfileResponse get(@PathVariable long profileId) {
        return profiles.getProfile(currentAccount.requireAccountId(), profileId);
    }

    @PostMapping
    public ResponseEntity<ProfileResponse> create(@Valid @RequestBody ProfileRequest request) {
        ProfileResponse created = profiles.createProfile(currentAccount.requireAccountId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{profileId}")
    public ProfileResponse update(@PathVariable long profileId, @Valid @RequestBody ProfileRequest request) {
        return profiles.updateProfile(currentAccount.requireAccountId(), profileId, request);
    }

    @PostMapping("/{profileId}/deactivate")
    public ProfileResponse deactivate(@PathVariable long profileId) {
        return profiles.deactivateProfile(currentAccount.requireAccountId(), profileId);
    }

    @PostMapping("/{profileId}/reactivate")
    public ProfileResponse reactivate(@PathVariable long profileId) {
        return profiles.reactivateProfile(currentAccount.requireAccountId(), profileId);
    }
}
