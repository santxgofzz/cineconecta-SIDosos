import { useId, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export function LoginPage() {
  const { login, isDemoMode } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const emailId = useId();
  const emailHintId = useId();
  const emailErrorId = useId();
  const passwordId = useId();
  const passwordErrorId = useId();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [touched, setTouched] = useState({ email: false, password: false });
  const [submitted, setSubmitted] = useState(false);
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const justRegistered = searchParams.get('registered') === '1';
  const justLoggedOut = searchParams.get('logout') === '1';

  const emailTrimmed = email.trim();
  const emailInvalid = (touched.email || submitted) && (!emailTrimmed || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(emailTrimmed));
  const passwordInvalid = (touched.password || submitted) && password.length === 0;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitted(true);
    setServerError('');

    if (!emailTrimmed || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(emailTrimmed) || !password) {
      return;
    }

    setSubmitting(true);
    try {
      await login({ email: emailTrimmed, password });
      navigate('/profiles');
    } catch (err) {
      setServerError(err.message || 'No se pudo iniciar sesión.');
    } finally {
      setSubmitting(false);
    }
  };

  const fillDemoAccount = () => {
    setEmail('familia@ejemplo.invalid');
    setPassword('SemillaLocal2026!');
    setServerError('');
  };

  return (
    <div className="cc-auth-page">
      <header className="cc-auth-header">
        <Link to="/" aria-label="CineConecta Inicio">
          <img
            src="/images/cineconecta-logo.png"
            alt="CineConecta"
            className="cc-logo-img cc-logo-auth"
            width="196"
            height="54"
          />
        </Link>
      </header>

      <main className="cc-auth-card">
        <h1>Iniciar sesión</h1>
        <p className="cc-auth-lead">Ingresa con tu cuenta de titular para elegir un perfil.</p>

        {justRegistered && (
          <p className="cc-alert cc-alert-success" role="status">
            Cuenta creada correctamente. Ya puedes iniciar sesión.
          </p>
        )}
        {justLoggedOut && (
          <p className="cc-alert cc-alert-info" role="status">
            Sesión cerrada correctamente.
          </p>
        )}
        {serverError && (
          <p className="cc-alert cc-alert-error" role="alert">
            Error: {serverError}
          </p>
        )}

        <form className="cc-form" onSubmit={handleSubmit} noValidate>
          <div className="cc-field">
            <label htmlFor={emailId}>Correo electrónico</label>
            <span id={emailHintId} className="cc-field-hint">
              Formato: nombre@correo.com
            </span>
            <input
              id={emailId}
              name="username"
              type="email"
              autoComplete="username"
              spellCheck={false}
              enterKeyHint="next"
              required
              maxLength={254}
              value={email}
              aria-invalid={emailInvalid ? 'true' : 'false'}
              aria-describedby={emailInvalid ? `${emailHintId} ${emailErrorId}` : emailHintId}
              className={emailInvalid ? 'user-invalid-fallback' : ''}
              onChange={(e) => setEmail(e.target.value)}
              onBlur={() => setTouched((prev) => ({ ...prev, email: true }))}
            />
            {emailInvalid && (
              <div id={emailErrorId} className="cc-field-error">
                Error: ingresa un correo electrónico válido (ejemplo: tu@correo.com).
              </div>
            )}
          </div>

          <div className="cc-field">
            <label htmlFor={passwordId}>Contraseña</label>
            <div className="cc-password-wrap">
              <input
                id={passwordId}
                name="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                enterKeyHint="done"
                required
                value={password}
                aria-invalid={passwordInvalid ? 'true' : 'false'}
                aria-describedby={passwordInvalid ? passwordErrorId : undefined}
                className={passwordInvalid ? 'user-invalid-fallback' : ''}
                onChange={(e) => setPassword(e.target.value)}
                onBlur={() => setTouched((prev) => ({ ...prev, password: true }))}
              />
              <button
                type="button"
                className="cc-toggle-pw"
                aria-pressed={showPassword}
                onClick={() => setShowPassword((prev) => !prev)}
              >
                {showPassword ? (
                  <>
                    <EyeOff size={16} aria-hidden="true" />
                    <span>Ocultar</span>
                  </>
                ) : (
                  <>
                    <Eye size={16} aria-hidden="true" />
                    <span>Mostrar</span>
                  </>
                )}
              </button>
            </div>
            {passwordInvalid && (
              <div id={passwordErrorId} className="cc-field-error">
                Error: ingresa tu contraseña para continuar.
              </div>
            )}
          </div>

          <button type="submit" className="cc-btn cc-btn-primary cc-btn-full" disabled={submitting}>
            {submitting ? 'Ingresando…' : 'Iniciar sesión'}
          </button>
        </form>

        {isDemoMode && (
          <div className="cc-demo-box">
            <p className="cc-demo-text">
              Modo demo local activo (sin Oracle). Usa la cuenta semilla <code>familia@ejemplo.invalid</code>:
            </p>
            <button type="button" className="cc-btn cc-btn-ghost cc-btn-full" onClick={fillDemoAccount}>
              Rellenar cuenta demo
            </button>
          </div>
        )}

        <p className="cc-auth-footer">
          ¿Primera vez en CineConecta? <Link to="/register">Crear cuenta</Link>
        </p>
      </main>
    </div>
  );
}
