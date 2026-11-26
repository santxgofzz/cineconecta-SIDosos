package co.edu.icesi.cineconecta.catalog;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.security.CurrentAccount;
import co.edu.icesi.cineconecta.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CatalogPageController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class CatalogPagesTest {
    @Autowired MockMvc mvc;
    @MockitoBean CatalogService catalog;
    @MockitoBean CurrentAccount accounts;

    @Test
    void searchRendersFiltersAndPreservesThemInPaginationLinks() throws Exception {
        var filters = new CatalogFilters("mar", "series", "Aventura", "es", 2023, 0, 1);
        var item = new CatalogService.Item("CNT-002", "Caminos del mar", "series",
                2023, 7, null, List.of("Aventura"));
        when(accounts.requireAccountId()).thenReturn(7L);
        when(catalog.search(7, 31, filters)).thenReturn(new CatalogService.Results(List.of(item), 0, 1, 2, 2, filters));
        mvc.perform(get("/profiles/31/catalog").with(user("titular")).param("q", "mar")
                        .param("type", "series").param("genre", "Aventura").param("language", "es")
                        .param("year", "2023").param("size", "1"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Caminos del mar")))
                .andExpect(content().string(containsString("page=1")))
                .andExpect(content().string(containsString("genre=Aventura")));
    }

    @Test
    void forbiddenProfileRendersTheErrorWithoutCatalog() throws Exception {
        when(accounts.requireAccountId()).thenReturn(7L);
        when(catalog.detail(7, 31, "CNT-002"))
                .thenThrow(ApiException.forbidden("PROFILE_NOT_ALLOWED", "El perfil no pertenece a tu cuenta."));
        mvc.perform(get("/profiles/31/catalog/CNT-002").with(user("titular")))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("El perfil no pertenece a tu cuenta.")));
    }

    @Test
    void detailRendersEpisodeLinksAndEscapesTitles() throws Exception {
        var item = new CatalogService.Item("CNT-002", "Caminos del mar", "series", 2023, 7,
                null, List.of("Aventura"));
        var episode = new CatalogService.Episode("EP-002-01", 1, "<script>Inicio</script>", "Sinopsis", 91);
        var season = new CatalogService.Season(1, "Primera temporada", List.of(episode));
        var detail = new CatalogService.Detail(item, "Sinopsis", List.of("CO"), List.of("es"), List.of(),
                null, null, List.of(season), List.of(), List.of(), List.of(), List.of());
        when(accounts.requireAccountId()).thenReturn(7L);
        when(catalog.detail(7, 31, "CNT-002")).thenReturn(detail);
        mvc.perform(get("/profiles/31/catalog/CNT-002").with(user("titular")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;Inicio&lt;/script&gt;")))
                .andExpect(content().string(containsString("episodeId=EP-002-01")))
                .andExpect(content().string(containsString("1 min 31 s")));
    }
}
