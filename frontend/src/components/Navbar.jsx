import { Link, useLocation, useNavigate } from 'react-router-dom';
import { LogIn, LogOut, SlidersHorizontal, UserPlus } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export function Navbar({ activeCategory, onSelectCategory }) {
  const { account, activeProfile, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const isHome = location.pathname === '/';

  const navItems = [
    { key: 'all', label: 'Inicio' },
    { key: 'movie', label: 'Películas' },
    { key: 'series', label: 'Series' },
    { key: 'documentary', label: 'Documentales' },
  ];

  const handleCategoryClick = (key) => {
    if (!isHome) {
      navigate('/');
      return;
    }
    onSelectCategory?.(key);
    const target = document.getElementById(key === 'all' ? 'hero' : 'catalogo');
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login?logout=1');
  };

  return (
    <header className="cc-nav">
      <div className="cc-nav-left">
        <Link className="cc-brand" to="/" aria-label="CineConecta Inicio">
          <img
            src="/images/cineconecta-logo.png"
            alt="CineConecta"
            className="cc-logo-img"
            width="154"
            height="42"
          />
        </Link>

        <nav aria-label="Navegación principal">
          <ul className="cc-nav-links">
            {navItems.map((item) => (
              <li key={item.key}>
                <button
                  type="button"
                  className="cc-nav-link"
                  aria-current={isHome && activeCategory === item.key ? 'page' : undefined}
                  onClick={() => handleCategoryClick(item.key)}
                >
                  {item.label}
                </button>
              </li>
            ))}
            {isHome && (
              <li>
                <a className="cc-nav-link" href="#planes">
                  Planes
                </a>
              </li>
            )}
            {account && (
              <li>
                <Link
                  className="cc-nav-link"
                  to="/profiles"
                  aria-current={location.pathname.startsWith('/profiles') ? 'page' : undefined}
                >
                  Perfiles
                </Link>
              </li>
            )}
          </ul>
        </nav>
      </div>

      <div className="cc-nav-actions">
        {account ? (
          <>
            <Link to="/profiles" className="cc-profile-chip" title="Cambiar perfil">
              {activeProfile ? (
                <>
                  <img
                    src={activeProfile.avatarUrl}
                    alt=""
                    className="cc-profile-chip-img"
                    width="32"
                    height="32"
                  />
                  <span>{activeProfile.name}</span>
                  <span className="cc-age-pill">{activeProfile.maxAgeRating}+</span>
                </>
              ) : (
                <span>Elegir perfil</span>
              )}
            </Link>
            <Link to="/profiles/manage" className="cc-btn cc-btn-ghost cc-btn-compact">
              <SlidersHorizontal size={16} aria-hidden="true" />
              <span>Administrar</span>
            </Link>
            <button type="button" className="cc-btn cc-btn-ghost cc-btn-compact" onClick={handleLogout}>
              <LogOut size={16} aria-hidden="true" />
              <span>Salir</span>
            </button>
          </>
        ) : (
          <>
            <Link to="/register" className="cc-btn cc-btn-ghost">
              <UserPlus size={16} aria-hidden="true" />
              <span>Crear cuenta</span>
            </Link>
            <Link to="/login" className="cc-btn cc-btn-primary">
              <LogIn size={16} aria-hidden="true" />
              <span>Iniciar sesión</span>
            </Link>
          </>
        )}
      </div>
    </header>
  );
}
