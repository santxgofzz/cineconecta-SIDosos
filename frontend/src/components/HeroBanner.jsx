import { Check, Play, Plus } from 'lucide-react';

export function HeroBanner({
  items,
  activeIndex,
  onSelectIndex,
  inMyList,
  onToggleMyList,
  onOpenDetail,
}) {
  const current = items[activeIndex] ?? items[0];

  return (
    <section id="hero" className="cc-hero" aria-label="Título destacado">
      <div className="cc-hero-media">
        <img
          src={current.heroImage}
          alt={current.title}
          className="cc-hero-img"
          fetchPriority="high"
          width="1280"
          height="720"
        />
        <div className="cc-hero-scrim" aria-hidden="true" />
      </div>

      <div className="cc-hero-content">
        <span className="cc-eyebrow">Original de CineConecta</span>
        <h1 className="cc-hero-title">{current.title}</h1>
        <div className="cc-hero-meta">
          <span>{current.year}</span>
          <span className="cc-age-pill">{current.ageRating}+</span>
          <span>{current.typeLabel}</span>
          <span>{current.durationLabel}</span>
        </div>
        <p className="cc-hero-synopsis">{current.synopsis}</p>
        <div className="cc-hero-cta">
          <button
            type="button"
            className="cc-btn cc-btn-light"
            onClick={() => onOpenDetail(current)}
          >
            <Play size={18} fill="currentColor" aria-hidden="true" />
            <span>Ver ahora</span>
          </button>
          <button
            type="button"
            className="cc-btn cc-btn-ghost"
            aria-pressed={inMyList}
            onClick={() => onToggleMyList(current.id)}
          >
            {inMyList ? (
              <>
                <Check size={18} aria-hidden="true" />
                <span>En Mi lista</span>
              </>
            ) : (
              <>
                <Plus size={18} aria-hidden="true" />
                <span>Mi lista</span>
              </>
            )}
          </button>
        </div>
      </div>

      <div className="cc-hero-dots" role="group" aria-label="Elegir destacado">
        {items.map((item, idx) => (
          <button
            key={item.id}
            type="button"
            className="cc-hero-dot"
            aria-label={`Destacar ${item.title}`}
            aria-pressed={idx === activeIndex}
            onClick={() => onSelectIndex(idx)}
          />
        ))}
      </div>
    </section>
  );
}
