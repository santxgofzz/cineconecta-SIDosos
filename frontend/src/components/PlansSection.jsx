export function PlansSection({ plans, onSelectPlan }) {
  return (
    <section id="planes" className="cc-section cc-container" aria-label="Planes disponibles">
      <div className="cc-section-header">
        <h2 className="cc-section-title">Elige tu plan CineConecta</h2>
      </div>
      <div className="cc-plans-grid">
        {plans.map((plan) => (
          <article key={plan.id} className={`cc-plan-card ${plan.featured ? 'is-featured' : ''}`}>
            <div>
              <div className="cc-plan-head">
                <h3 className="cc-plan-name">{plan.name}</h3>
                {plan.featured && <span className="cc-plan-tag">Más elegido</span>}
              </div>
              <p className="cc-plan-price">
                {plan.priceFormatted} <span>/ 30 días</span>
              </p>
              <ul className="cc-plan-features">
                {plan.features.map((feat) => (
                  <li key={feat}>{feat}</li>
                ))}
              </ul>
            </div>
            <button
              type="button"
              className={`cc-btn ${plan.featured ? 'cc-btn-primary' : 'cc-btn-ghost'}`}
              onClick={() => onSelectPlan(plan)}
            >
              Elegir {plan.name} ({plan.maxProfiles} {plan.maxProfiles === 1 ? 'perfil' : 'perfiles'})
            </button>
          </article>
        ))}
      </div>
    </section>
  );
}
