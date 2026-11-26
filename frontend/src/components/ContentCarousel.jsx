import { useEffect, useId, useRef, useState } from 'react';
import { Check, ChevronLeft, ChevronRight } from 'lucide-react';

export function ContentCarousel({ id = 'catalogo', title, subtitle, items, myListIds, onOpenDetail, variant = 'landscape' }) {
  const headingId = useId();
  const trackRef = useRef(null);
  const prevBtnRef = useRef(null);
  const nextBtnRef = useRef(null);
  const hasInteractedRef = useRef(false);

  const [currentSlide, setCurrentSlide] = useState(0);
  const [canPrev, setCanPrev] = useState(false);
  const [canNext, setCanNext] = useState(items.length > 1);
  const [statusMessage, setStatusMessage] = useState('');

  useEffect(() => {
    const track = trackRef.current;
    if (!track) return;
    track.scrollLeft = 0;
    hasInteractedRef.current = false;
    setCurrentSlide(0);
    setStatusMessage('');
    setCanPrev(false);
    setCanNext(items.length > 1);
  }, [items]);

  useEffect(() => {
    const track = trackRef.current;
    if (!track) return;

    const syncFromScroll = () => {
      const children = Array.from(track.children);
      if (children.length === 0) return;
      const trackLeft = track.scrollLeft;
      let nearest = 0;
      let bestDist = Infinity;
      children.forEach((child, idx) => {
        const dist = Math.abs(child.offsetLeft - trackLeft);
        if (dist < bestDist) {
          bestDist = dist;
          nearest = idx;
        }
      });

      const atStart = track.scrollLeft <= 2;
      const atEnd = track.scrollLeft + track.clientWidth >= track.scrollWidth - 2;
      setCanPrev(!atStart);
      setCanNext(!atEnd && children.length > 1);

      if (atStart && document.activeElement === prevBtnRef.current) {
        nextBtnRef.current?.focus();
      } else if (atEnd && document.activeElement === nextBtnRef.current) {
        prevBtnRef.current?.focus();
      }

      setCurrentSlide((prev) => {
        if (hasInteractedRef.current && prev !== nearest) {
          setStatusMessage(`Título ${nearest + 1} de ${children.length}: ${items[nearest]?.title ?? ''}`);
        }
        return nearest;
      });
    };

    let timerId;
    const onScrollFallback = () => {
      clearTimeout(timerId);
      timerId = setTimeout(syncFromScroll, 120);
    };

    const supportsScrollEnd = 'onscrollend' in window;
    if (supportsScrollEnd) {
      track.addEventListener('scrollend', syncFromScroll);
    } else {
      track.addEventListener('scroll', onScrollFallback, { passive: true });
    }
    window.addEventListener('resize', syncFromScroll);

    return () => {
      clearTimeout(timerId);
      if (supportsScrollEnd) {
        track.removeEventListener('scrollend', syncFromScroll);
      } else {
        track.removeEventListener('scroll', onScrollFallback);
      }
      window.removeEventListener('resize', syncFromScroll);
    };
  }, [items]);

  const scrollByStep = (direction) => {
    const track = trackRef.current;
    if (!track) return;
    hasInteractedRef.current = true;
    const children = Array.from(track.children);
    const nextIdx = Math.max(0, Math.min(children.length - 1, currentSlide + direction));
    const target = children[nextIdx];
    if (target) {
      track.scrollTo({ left: target.offsetLeft });
    }
  };

  if (items.length === 0) return null;

  const isPortrait = variant === 'portrait';

  return (
    <section
      id={id}
      className="cc-section cc-container cc-carousel"
      aria-roledescription="carousel"
      aria-labelledby={headingId}
    >
      <div className="cc-section-header">
        <div>
          <h2 id={headingId} className="cc-section-title">{title}</h2>
          {subtitle && <p className="cc-section-sub">{subtitle}</p>}
        </div>
        <div className="cc-carousel-controls">
          <button
            ref={prevBtnRef}
            type="button"
            className="cc-carousel-btn"
            aria-label="Título anterior"
            disabled={!canPrev}
            onClick={() => scrollByStep(-1)}
          >
            <ChevronLeft size={20} aria-hidden="true" />
          </button>
          <button
            ref={nextBtnRef}
            type="button"
            className="cc-carousel-btn"
            aria-label="Siguiente título"
            disabled={!canNext}
            onClick={() => scrollByStep(1)}
          >
            <ChevronRight size={20} aria-hidden="true" />
          </button>
        </div>
      </div>

      <div ref={trackRef} className="cc-carousel-track" tabIndex={0} role="group" aria-label="Títulos disponibles">
        {items.map((item, idx) => (
          <div
            key={item.id}
            className={`cc-slide ${isPortrait ? 'is-portrait' : ''}`}
            role="group"
            aria-roledescription="slide"
            aria-label={`${idx + 1} de ${items.length}`}
          >
            <button type="button" className="cc-card" onClick={() => onOpenDetail(item)}>
              <div className={`cc-card-thumb ${isPortrait ? 'is-portrait' : ''}`}>
                <img
                  src={item.cardImage}
                  alt={item.title}
                  loading="lazy"
                  referrerPolicy="no-referrer"
                  width={isPortrait ? 420 : 640}
                  height={isPortrait ? 590 : 360}
                  onError={(e) => {
                    e.currentTarget.onerror = null;
                    e.currentTarget.src = '/images/poster-viento.jpg';
                  }}
                />
                <span className="cc-card-badge">
                  {myListIds.includes(item.id) ? (
                    <>
                      <Check size={13} aria-hidden="true" />
                      <span>Mi lista</span>
                    </>
                  ) : (
                    `${item.ageRating}+`
                  )}
                </span>
              </div>
              <div className="cc-card-body">
                <h3 className="cc-card-title">{item.title}</h3>
                <p className="cc-card-meta">
                  {item.typeLabel} · {item.year} · {item.genres.join(', ')}
                </p>
              </div>
            </button>
          </div>
        ))}
      </div>

      <p className="visually-hidden" role="status" aria-live="polite">
        {statusMessage}
      </p>
    </section>
  );
}
