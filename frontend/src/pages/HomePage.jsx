import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Check, Plus, Search, X } from 'lucide-react';
import { CATALOG_ITEMS, CATEGORY_TILES, SUBSCRIPTION_PLANS, mapBackendCatalogItem } from '../data/catalogPreview';
import { useAuth } from '../context/AuthContext';
import { Navbar } from '../components/Navbar';
import { HeroBanner } from '../components/HeroBanner';
import { CategoryTiles } from '../components/CategoryTiles';
import { ContentCarousel } from '../components/ContentCarousel';
import { PlansSection } from '../components/PlansSection';
import { Footer } from '../components/Footer';

export function HomePage() {
  const { activeProfile, account, isDemoMode } = useAuth();
  const searchId = useId();

  const [heroIndex, setHeroIndex] = useState(0);
  const [activeCategory, setActiveCategory] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [myListIds, setMyListIds] = useState(['MV-tt0076759', 'CNT-001', 'CNT-002']);
  const [backendItems, setBackendItems] = useState(null);
  const [detailItem, setDetailItem] = useState(null);
  const [toast, setToast] = useState('');
  const dialogRef = useRef(null);

  useEffect(() => {
    if (isDemoMode || !activeProfile) {
      setBackendItems(null);
      return;
    }

    const controller = new AbortController();
    fetch(`/api/profiles/${activeProfile.profileId}/catalog?size=100`, {
      credentials: 'include',
      headers: { Accept: 'application/json' },
      signal: controller.signal,
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (data?.items) {
          setBackendItems(data.items.map(mapBackendCatalogItem));
        }
      })
      .catch(() => {
        // Si no hay conexión con el backend real, se mantiene la semilla exacta de MongoDB
      });

    return () => controller.abort();
  }, [isDemoMode, activeProfile]);

  const maxAge = activeProfile ? activeProfile.maxAgeRating : 18;

  const allowedCatalog = useMemo(() => {
    const source = backendItems ?? CATALOG_ITEMS;
    return source.filter((item) => item.ageRating <= maxAge);
  }, [backendItems, maxAge]);

  const featuredItems = useMemo(() => {
    const featured = allowedCatalog.filter((item) => item.featured);
    return featured.length > 0 ? featured : allowedCatalog.slice(0, 3);
  }, [allowedCatalog]);

  const trimmedQuery = searchQuery.trim().toLowerCase();

  const filteredItems = useMemo(() => {
    return allowedCatalog.filter((item) => {
      const matchesCat =
        activeCategory === 'all' ||
        (activeCategory === 'kids' ? item.ageRating <= 7 : item.type === activeCategory);
      if (!matchesCat) return false;
      if (!trimmedQuery) return true;
      return (
        item.title.toLowerCase().includes(trimmedQuery) ||
        item.genres.some((g) => g.toLowerCase().includes(trimmedQuery)) ||
        (item.director && item.director.toLowerCase().includes(trimmedQuery))
      );
    });
  }, [activeCategory, allowedCatalog, trimmedQuery]);

  const popularMovies = useMemo(
    () => allowedCatalog.filter((item) => !item.isOriginal && item.type === 'movie').slice(0, 18),
    [allowedCatalog]
  );

  const actionSciFiMovies = useMemo(
    () =>
      allowedCatalog.filter(
        (item) =>
          !item.isOriginal &&
          item.genres.some((g) => ['Action', 'Sci-Fi', 'Adventure', 'Fantasy'].includes(g))
      ),
    [allowedCatalog]
  );

  const docsAndSeries = useMemo(
    () => allowedCatalog.filter((item) => item.type === 'documentary' || item.type === 'series'),
    [allowedCatalog]
  );

  const originalItems = useMemo(
    () => allowedCatalog.filter((item) => item.isOriginal),
    [allowedCatalog]
  );

  const carouselTitle = useMemo(() => {
    const tile = CATEGORY_TILES.find((t) => t.key === activeCategory);
    if (trimmedQuery) return `Resultados para "${searchQuery.trim()}" (${filteredItems.length})`;
    return tile ? `${tile.label} (${filteredItems.length})` : 'Catálogo CineConecta';
  }, [activeCategory, filteredItems.length, searchQuery, trimmedQuery]);

  useEffect(() => {
    if (heroIndex >= featuredItems.length) {
      setHeroIndex(0);
    }
  }, [featuredItems.length, heroIndex]);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (detailItem && !dialog.open) {
      dialog.showModal();
    } else if (!detailItem && dialog.open) {
      dialog.close();
    }
  }, [detailItem]);

  useEffect(() => {
    if (!toast) return;
    const id = setTimeout(() => setToast(''), 4200);
    return () => clearTimeout(id);
  }, [toast]);

  const toggleMyList = (contentId) => {
    setMyListIds((prev) => {
      const exists = prev.includes(contentId);
      setToast(exists ? 'Quitado de Mi lista.' : 'Agregado a Mi lista.');
      return exists ? prev.filter((id) => id !== contentId) : [...prev, contentId];
    });
  };

  const showGroupedRows = activeCategory === 'all' && !trimmedQuery;

  return (
    <>
      <Navbar activeCategory={activeCategory} onSelectCategory={setActiveCategory} />

      <main className="cc-main">
        {account && activeProfile && (
          <div className="cc-active-banner cc-container">
            <span>
              Viendo como <strong>{activeProfile.name}</strong> · Clasificación máxima{' '}
              <strong>{activeProfile.maxAgeRating}+</strong>
            </span>
            <Link to="/profiles" className="cc-link-inline">
              Cambiar perfil
            </Link>
          </div>
        )}

        {featuredItems.length > 0 && (
          <HeroBanner
            items={featuredItems}
            activeIndex={heroIndex}
            onSelectIndex={setHeroIndex}
            inMyList={myListIds.includes(featuredItems[heroIndex]?.id)}
            onToggleMyList={toggleMyList}
            onOpenDetail={setDetailItem}
          />
        )}

        <CategoryTiles
          tiles={CATEGORY_TILES}
          activeCategory={activeCategory}
          onSelectCategory={setActiveCategory}
        />

        <section className="cc-search-bar cc-container" aria-label="Buscar en el catálogo">
          <div className="cc-search-wrap">
            <Search size={18} className="cc-search-icon" aria-hidden="true" />
            <label htmlFor={searchId} className="visually-hidden">
              Buscar por título, género o director
            </label>
            <input
              id={searchId}
              type="search"
              placeholder="Buscar películas, series, documentales, género o director…"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            {searchQuery && (
              <button
                type="button"
                className="cc-search-clear"
                aria-label="Limpiar búsqueda"
                onClick={() => setSearchQuery('')}
              >
                <X size={16} aria-hidden="true" />
              </button>
            )}
          </div>
        </section>

        {showGroupedRows ? (
          <>
            <ContentCarousel
              id="catalogo"
              title="Populares en CineConecta"
              subtitle={`Películas reales cargadas en cineconecta.contents (${popularMovies.length})`}
              items={popularMovies}
              myListIds={myListIds}
              onOpenDetail={setDetailItem}
              variant="portrait"
            />

            <ContentCarousel
              id="accion-scifi"
              title="Acción, Aventura y Ciencia Ficción"
              items={actionSciFiMovies}
              myListIds={myListIds}
              onOpenDetail={setDetailItem}
              variant="portrait"
            />

            <ContentCarousel
              id="docs-series"
              title="Documentales y Series"
              items={docsAndSeries}
              myListIds={myListIds}
              onOpenDetail={setDetailItem}
              variant="portrait"
            />

            <ContentCarousel
              id="originales"
              title="Originales de CineConecta"
              items={originalItems}
              myListIds={myListIds}
              onOpenDetail={setDetailItem}
              variant="landscape"
            />
          </>
        ) : (
          <ContentCarousel
            id="catalogo"
            title={carouselTitle}
            subtitle={
              activeProfile
                ? `Contenidos publicados visibles para ${activeProfile.name} (${maxAge}+)`
                : 'Catálogo publicado en MongoDB (cineconecta.contents)'
            }
            items={filteredItems}
            myListIds={myListIds}
            onOpenDetail={setDetailItem}
            variant={activeCategory === 'series' ? 'landscape' : 'portrait'}
          />
        )}

        {!showGroupedRows && filteredItems.length === 0 && (
          <section className="cc-container cc-empty-search">
            <p>No se encontraron títulos que coincidan con tu búsqueda y clasificación del perfil.</p>
          </section>
        )}

        <PlansSection
          plans={SUBSCRIPTION_PLANS}
          onSelectPlan={(plan) =>
            setToast(`Plan ${plan.name} (${plan.priceFormatted}) permite hasta ${plan.maxProfiles} perfil(es).`)
          }
        />
      </main>

      <Footer />

      <dialog
        ref={dialogRef}
        className="cc-dialog"
        aria-labelledby="cc-dialog-title"
        onClose={() => setDetailItem(null)}
        onClick={(e) => {
          if (e.target === dialogRef.current) setDetailItem(null);
        }}
      >
        {detailItem && (
          <>
            <img
              src={detailItem.heroImage}
              alt={detailItem.title}
              className="cc-dialog-media"
              referrerPolicy="no-referrer"
              width="640"
              height="360"
              onError={(e) => {
                e.currentTarget.onerror = null;
                e.currentTarget.src = '/images/hero-viento.jpg';
              }}
            />
            <div className="cc-dialog-body">
              <span className="cc-eyebrow">{detailItem.typeLabel} · {detailItem.id}</span>
              <h3 id="cc-dialog-title">{detailItem.title}</h3>
              <div className="cc-hero-meta">
                <span>{detailItem.year}</span>
                <span className="cc-age-pill">{detailItem.ageRating}+</span>
                <span>{detailItem.durationLabel}</span>
                {detailItem.imdbRating ? <span>IMDb {detailItem.imdbRating}</span> : null}
                <span>{detailItem.genres.join(' · ')}</span>
              </div>
              {detailItem.director && (
                <p className="cc-dialog-director">Dirección: {detailItem.director}</p>
              )}
              <p className="cc-hero-synopsis">{detailItem.synopsis}</p>
              <div className="cc-hero-cta">
                <button
                  type="button"
                  className="cc-btn cc-btn-primary"
                  onClick={() => toggleMyList(detailItem.id)}
                >
                  {myListIds.includes(detailItem.id) ? (
                    <>
                      <Check size={18} aria-hidden="true" />
                      <span>Quitar de Mi lista</span>
                    </>
                  ) : (
                    <>
                      <Plus size={18} aria-hidden="true" />
                      <span>Agregar a Mi lista</span>
                    </>
                  )}
                </button>
                <button
                  type="button"
                  className="cc-btn cc-btn-ghost"
                  onClick={() => setDetailItem(null)}
                >
                  <X size={18} aria-hidden="true" />
                  <span>Cerrar</span>
                </button>
              </div>
            </div>
          </>
        )}
      </dialog>

      {toast && (
        <div className="cc-toast" role="status" aria-live="polite">
          {toast}
        </div>
      )}
    </>
  );
}
