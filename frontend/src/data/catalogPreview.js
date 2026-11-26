import moviesDataset from './moviesDataset.json';

export const LOCAL_COVER_MAP = {
  'CNT-001': { heroImage: '/images/hero-viento.jpg', cardImage: '/images/poster-viento.jpg' },
  'CNT-002': { heroImage: '/images/poster-mar.jpg', cardImage: '/images/poster-mar.jpg' },
  'CNT-003': { heroImage: '/images/poster-semillas.jpg', cardImage: '/images/poster-semillas.jpg' },
  'CNT-004': { heroImage: '/images/poster-ciudad.jpg', cardImage: '/images/poster-ciudad.jpg' },
  'CNT-007': { heroImage: '/images/poster-agua.jpg', cardImage: '/images/poster-agua.jpg' },
  'CNT-008': { heroImage: '/images/poster-codigo.jpg', cardImage: '/images/poster-codigo.jpg' },
};

export const ORIGINAL_ITEMS = [
  {
    id: 'CNT-001',
    title: 'La ruta del viento',
    type: 'movie',
    typeLabel: 'Película',
    year: 2022,
    ageRating: 12,
    durationLabel: '1 h 37 min',
    genres: ['Drama'],
    director: 'Ana Torres',
    featured: true,
    isOriginal: true,
    heroImage: '/images/hero-viento.jpg',
    cardImage: '/images/poster-viento.jpg',
    synopsis: 'Una familia recorre distintas regiones del país.',
  },
  {
    id: 'CNT-002',
    title: 'Caminos del mar',
    type: 'series',
    typeLabel: 'Serie',
    year: 2023,
    ageRating: 7,
    durationLabel: '2 temporadas · 3 episodios',
    genres: ['Aventura'],
    featured: true,
    isOriginal: true,
    heroImage: '/images/poster-mar.jpg',
    cardImage: '/images/poster-mar.jpg',
    synopsis: 'Historias de una comunidad costera.',
  },
  {
    id: 'CNT-003',
    title: 'Semillas de futuro',
    type: 'documentary',
    typeLabel: 'Documental',
    year: 2024,
    ageRating: 0,
    durationLabel: '55 min',
    genres: ['Ciencia'],
    director: 'María Rojas',
    featured: true,
    isOriginal: true,
    heroImage: '/images/poster-semillas.jpg',
    cardImage: '/images/poster-semillas.jpg',
    synopsis: 'Experiencias sobre agricultura sostenible.',
  },
  {
    id: 'CNT-004',
    title: 'La ciudad invisible',
    type: 'movie',
    typeLabel: 'Película',
    year: 2021,
    ageRating: 18,
    durationLabel: '2 h 00 min',
    genres: ['Suspenso'],
    director: 'Julián Méndez',
    featured: false,
    isOriginal: true,
    heroImage: '/images/poster-ciudad.jpg',
    cardImage: '/images/poster-ciudad.jpg',
    synopsis: 'Un misterio policial se desarrolla en la ciudad.',
  },
  {
    id: 'CNT-007',
    title: 'Agua para todos',
    type: 'documentary',
    typeLabel: 'Documental',
    year: 2025,
    ageRating: 12,
    durationLabel: '1 h 00 min',
    genres: ['Ciencia', 'Sociedad'],
    director: 'Valeria Ospina',
    featured: false,
    isOriginal: true,
    heroImage: '/images/poster-agua.jpg',
    cardImage: '/images/poster-agua.jpg',
    synopsis: 'Una investigación sobre fuentes de agua.',
  },
  {
    id: 'CNT-008',
    title: 'Código nocturno',
    type: 'series',
    typeLabel: 'Serie',
    year: 2026,
    ageRating: 13,
    durationLabel: '1 temporada · 2 episodios',
    genres: ['Ciencia ficción', 'Suspenso'],
    featured: false,
    isOriginal: true,
    heroImage: '/images/poster-codigo.jpg',
    cardImage: '/images/poster-codigo.jpg',
    synopsis: 'Un grupo resuelve problemas tecnológicos.',
  },
];

/**
 * Catálogo completo publicado en cineconecta.contents:
 * 48 películas/documentales reales de loadMovieDetailsDataset.js + 6 originales CNT-*.
 */
export const CATALOG_ITEMS = [...moviesDataset, ...ORIGINAL_ITEMS];

export function mapBackendCatalogItem(item) {
  const known = CATALOG_ITEMS.find((c) => c.id === item.id);
  const localCovers = LOCAL_COVER_MAP[item.id];
  const hasRealCover = item.coverUrl && !item.coverUrl.includes('example.org');
  const typeMap = { movie: 'Película', series: 'Serie', documentary: 'Documental' };

  return {
    id: item.id,
    title: item.title,
    type: item.type,
    typeLabel: typeMap[item.type] || item.type,
    year: item.releaseYear ?? known?.year ?? 2024,
    ageRating: item.ageRating ?? known?.ageRating ?? 0,
    durationLabel: known?.durationLabel ?? 'Disponible en CineConecta',
    genres: item.genres?.length ? item.genres : known?.genres ?? [],
    director: known?.director,
    imdbRating: known?.imdbRating,
    featured: known?.featured ?? false,
    isOriginal: known?.isOriginal ?? item.id.startsWith('CNT-'),
    heroImage: hasRealCover ? item.coverUrl : localCovers?.heroImage || '/images/hero-viento.jpg',
    cardImage: hasRealCover ? item.coverUrl : localCovers?.cardImage || '/images/poster-viento.jpg',
    synopsis: known?.synopsis ?? 'Título publicado en el catálogo de CineConecta.',
  };
}

export const CATEGORY_TILES = [
  { key: 'all', label: 'Todo CineConecta', sub: '54 títulos publicados', image: '/images/hero-viento.jpg' },
  {
    key: 'movie',
    label: 'Películas',
    sub: 'Cine y estrenos',
    image: moviesDataset[0]?.cardImage || '/images/poster-ciudad.jpg',
  },
  { key: 'series', label: 'Series', sub: 'Temporadas y episodios', image: '/images/poster-codigo.jpg' },
  { key: 'documentary', label: 'Documentales', sub: 'Ciencia y sociedad', image: '/images/poster-semillas.jpg' },
  {
    key: 'kids',
    label: 'Infantil 7+',
    sub: 'Apto para niños',
    image: moviesDataset.find((m) => m.ageRating <= 7)?.cardImage || '/images/poster-mar.jpg',
  },
];

export const SUBSCRIPTION_PLANS = [
  {
    id: 1,
    name: 'Básico',
    priceFormatted: '$29.900',
    maxProfiles: 1,
    featured: false,
    features: ['1 perfil activo en tu cuenta', 'Catálogo completo según edad', 'Historial y Mi lista personal'],
  },
  {
    id: 2,
    name: 'Familiar',
    priceFormatted: '$49.900',
    maxProfiles: 3,
    featured: true,
    features: ['Hasta 3 perfiles simultáneos', 'Control parental por edad (0+ a 18+)', 'Progreso independiente por perfil'],
  },
  {
    id: 3,
    name: 'Premium',
    priceFormatted: '$69.900',
    maxProfiles: 5,
    featured: false,
    features: ['Hasta 5 perfiles activos', 'Todos los estrenos y documentales', 'Renovación de 30 días sin solapamientos'],
  },
];
