import { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { LogOut, Plus, SlidersHorizontal } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export function ProfileSelectPage() {
  const { loading, account, profiles, selectProfile, logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!loading && !account) {
      navigate('/login', { replace: true });
    }
  }, [loading, account, navigate]);

  if (loading || !account) return null;

  const activeProfiles = profiles.filter((p) => p.active);

  const handleChoose = (profile) => {
    selectProfile(profile);
    navigate('/');
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login?logout=1');
  };

  return (
    <div className="cc-profiles-page">
      <header className="cc-profiles-header">
        <Link to="/" aria-label="CineConecta Inicio">
          <img
            src="/images/cineconecta-logo.png"
            alt="CineConecta"
            className="cc-logo-img"
            width="160"
            height="44"
          />
        </Link>

        <div className="cc-nav-actions">
          <Link to="/profiles/manage" className="cc-btn cc-btn-ghost">
            <SlidersHorizontal size={16} aria-hidden="true" />
            <span>Administrar perfiles</span>
          </Link>
          <button type="button" className="cc-btn cc-btn-ghost" onClick={handleLogout}>
            <LogOut size={16} aria-hidden="true" />
            <span>Cerrar sesión</span>
          </button>
        </div>
      </header>

      <main className="cc-profiles-stage">
        <h1>¿Quién está viendo?</h1>
        <p className="cc-profiles-sub">
          Cuenta: <strong>{account.fullName}</strong> · Plan {account.planName}
        </p>

        <div className="cc-avatar-grid">
          {activeProfiles.map((profile) => (
            <button
              key={profile.profileId}
              type="button"
              className="cc-avatar-card"
              onClick={() => handleChoose(profile)}
            >
              <span className="cc-avatar-ring">
                <img
                  src={profile.avatarUrl}
                  alt={profile.name}
                  className="cc-avatar-img"
                  width="148"
                  height="148"
                />
              </span>
              <span className="cc-avatar-name">{profile.name}</span>
              <span className="cc-age-pill">{profile.maxAgeRating}+</span>
            </button>
          ))}

          <Link to="/profiles/manage" className="cc-avatar-card cc-avatar-add">
            <span className="cc-avatar-ring cc-avatar-plus" aria-hidden="true">
              <Plus size={36} strokeWidth={1.75} />
            </span>
            <span className="cc-avatar-name">Agregar perfil</span>
            <span className="cc-avatar-hint">
              {activeProfiles.length} / {account.maxProfiles} activos
            </span>
          </Link>
        </div>
      </main>
    </div>
  );
}
