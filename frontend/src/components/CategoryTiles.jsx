export function CategoryTiles({ tiles, activeCategory, onSelectCategory }) {
  return (
    <section className="cc-tiles-section cc-container" aria-label="Explorar por categoría">
      <div className="cc-tiles-grid">
        {tiles.map((tile) => (
          <button
            key={tile.key}
            type="button"
            className="cc-tile"
            aria-pressed={activeCategory === tile.key}
            onClick={() => onSelectCategory(tile.key)}
          >
            <img
              src={tile.image}
              alt=""
              aria-hidden="true"
              loading="lazy"
              width="320"
              height="180"
              className="cc-tile-bg"
            />
            <span className="cc-tile-scrim" aria-hidden="true" />
            <span className="cc-tile-label">{tile.label}</span>
            <span className="cc-tile-sub">{tile.sub}</span>
          </button>
        ))}
      </div>
    </section>
  );
}
