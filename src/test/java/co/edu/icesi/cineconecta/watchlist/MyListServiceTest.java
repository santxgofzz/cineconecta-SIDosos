package co.edu.icesi.cineconecta.watchlist;

import co.edu.icesi.cineconecta.common.error.ApiException;
import co.edu.icesi.cineconecta.common.error.ErrorCodes;
import co.edu.icesi.cineconecta.common.resilience.TransientRetry;
import co.edu.icesi.cineconecta.integration.AccountDataGateway;
import co.edu.icesi.cineconecta.integration.CatalogEntry;
import co.edu.icesi.cineconecta.integration.CatalogGateway;
import co.edu.icesi.cineconecta.integration.ProfileAccess;
import co.edu.icesi.cineconecta.integration.ProfileGuard;
import co.edu.icesi.cineconecta.persistence.mongo.document.MyListItemDocument;
import co.edu.icesi.cineconecta.persistence.mongo.repository.MyListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MyListServiceTest {
    static final long ACCOUNT = 1L, PROFILE = 15L;
    static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    MyListRepository repo = mock(MyListRepository.class);
    CatalogGateway catalog = mock(CatalogGateway.class);
    AccountDataGateway oracle = mock(AccountDataGateway.class);
    MyListService service;

    static CatalogEntry movie(String id, String status, int age) {
        return new CatalogEntry(id, "movie", "Título " + id, status, age, 5400, null);
    }

    @BeforeEach
    void setUp() {
        service = new MyListService(repo, catalog, new ProfileGuard(oracle), new TransientRetry(3, 0),
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(oracle.findProfile(PROFILE)).thenReturn(Optional.of(new ProfileAccess(PROFILE, ACCOUNT, 13, true)));
        when(repo.insert(any(MyListItemDocument.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void addsPublishedContent() {
        when(catalog.findById("CNT-1")).thenReturn(Optional.of(movie("CNT-1", "published", 12)));
        when(repo.findByProfileIdAndContentId(PROFILE, "CNT-1")).thenReturn(Optional.empty());

        MyListService.AddResult r = service.add(ACCOUNT, PROFILE, "CNT-1");

        assertTrue(r.created());
        assertEquals(MyListEntryView.AVAILABLE, r.entry().availability());
        verify(repo).insert(any(MyListItemDocument.class));
    }

    @Test
    void addingTwiceDoesNotDuplicate() {
        when(catalog.findById("CNT-1")).thenReturn(Optional.of(movie("CNT-1", "published", 12)));
        when(repo.findByProfileIdAndContentId(PROFILE, "CNT-1"))
                .thenReturn(Optional.of(new MyListItemDocument(PROFILE, "CNT-1", NOW)));

        MyListService.AddResult r = service.add(ACCOUNT, PROFILE, "CNT-1");

        assertFalse(r.created());
        verify(repo, never()).insert(any(MyListItemDocument.class));
    }

    @Test
    void concurrentAddResolvedByUniqueIndex() {
        when(catalog.findById("CNT-1")).thenReturn(Optional.of(movie("CNT-1", "published", 12)));
        when(repo.findByProfileIdAndContentId(PROFILE, "CNT-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new MyListItemDocument(PROFILE, "CNT-1", NOW)));
        when(repo.insert(any(MyListItemDocument.class))).thenThrow(new DuplicateKeyException("E11000"));

        MyListService.AddResult r = service.add(ACCOUNT, PROFILE, "CNT-1");

        assertFalse(r.created());
        assertEquals("CNT-1", r.entry().contentId());
    }

    @Test
    void retiredContentCannotBeAdded() {
        when(catalog.findById("CNT-9")).thenReturn(Optional.of(movie("CNT-9", "retired", 0)));

        ApiException ex = assertThrows(ApiException.class, () -> service.add(ACCOUNT, PROFILE, "CNT-9"));
        assertEquals(ErrorCodes.CONTENT_NOT_AVAILABLE, ex.code());
    }

    @Test
    void listDistinguishesRetiredAndMissingTitles() {
        when(repo.findByProfileIdOrderByAddedAtDesc(PROFILE)).thenReturn(List.of(
                new MyListItemDocument(PROFILE, "CNT-1", NOW),
                new MyListItemDocument(PROFILE, "CNT-9", NOW),
                new MyListItemDocument(PROFILE, "CNT-X", NOW)));
        when(catalog.findByIds(any())).thenReturn(Map.of(
                "CNT-1", movie("CNT-1", "published", 0),
                "CNT-9", movie("CNT-9", "retired", 0)));

        List<MyListEntryView> list = service.list(ACCOUNT, PROFILE);

        assertEquals(MyListEntryView.AVAILABLE, list.get(0).availability());
        assertEquals(MyListEntryView.RETIRED, list.get(1).availability());
        assertFalse(list.get(1).available());
        assertEquals("Título CNT-9", list.get(1).title());
        assertEquals(MyListEntryView.UNAVAILABLE, list.get(2).availability());
    }

    @Test
    void removeIsIdempotentAndWorksForRetiredContent() {
        when(repo.deleteByProfileIdAndContentId(PROFILE, "CNT-9")).thenReturn(1L).thenReturn(0L);

        assertTrue(service.remove(ACCOUNT, PROFILE, "CNT-9"));
        assertFalse(service.remove(ACCOUNT, PROFILE, "CNT-9"));
        verifyNoInteractions(catalog);
    }
}
