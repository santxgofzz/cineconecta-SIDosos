import { useId, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const nameId = useId();
  const nameErrorId = useId();
  const emailId = useId();
  const emailHintId = useId();
  const emailErrorId = useId();
  const passwordId = useId();
  const passwordRulesId = useId();
  const passwordErrorId = useId();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [touched, setTouched] = useState({ fullName: false, email: false, password: false });
  const [submitted, setSubmitted] = useState(false);
  const [serverError, setServerError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const cleanName = fullName.trim();
  const cleanEmail = email.trim();

  const nameInvalid = (touched.fullName || submitted) && (cleanName.length === 0 || cleanName.length > 150);
  const emailInvalid =
    (touched.email || submitted) && (!cleanEmail || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(cleanEmail));
  const passwordInvalid = (touched.password || submitted) && (password.length < 8 || password.length > 72);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitted(true);
    setServerError('');

    if (
      cleanName.length === 0 ||
      cleanName.length > 150 ||
      !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(cleanEmail) ||
      password.length < 8 ||
      password.length > 72
    ) {
      return;
    }

    setSubmitting(true);
    try {
      await register({ fullName: cleanName, email: cleanEmail, password });
      navigate('/login?registered=1');
    } catch (err) {
      setServerError(err.message || 'No se pudo completar el registro.');
    } finally {
      setSubmitting(false);
    }
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
        <h1>Crear cuenta</h1>
        <p className="cc-auth-lead">Regístrate como titular para administrar tus perfiles y disfrutar el catálogo.</p>

        {serverError && (
          <p className="cc-alert cc-alert-error" role="alert">
            Error: {serverError}
          </p>
        )}

        <form className="cc-form" onSubmit={handleSubmit} noValidate>
          <div className="cc-field">
            <label htmlFor={nameId}>Nombre completo</label>
            <input
              id={nameId}
              name="fullName"
              type="text"
              autoComplete="name"
              enterKeyHint="next"
              required
              maxLength={150}
              value={fullName}
              aria-invalid={nameInvalid ? 'true' : 'false'}
              aria-describedby={nameInvalid ? nameErrorId : undefined}
              className={nameInvalid ? 'user-invalid-fallback' : ''}
              onChange={(e) => setFullName(e.target.value)}
              onBlur={() => setTouched((prev) => ({ ...prev, fullName: true }))}
            />
            {nameInvalid && (
              <div id={nameErrorId} className="cc-field-error">
                Error: ingresa tu nombre completo (máximo 150 caracteres).
              </div>
            )}
          </div>

          <div className="cc-field">
            <label htmlFor={emailId}>Correo electrónico</label>
            <span id={emailHintId} className="cc-field-hint">
              Formato: nombre@correo.com (debe ser único)
            </span>
            <input
              id={emailId}
              name="email"
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
                Error: ingresa una dirección de correo válida.
              </div>
            )}
          </div>

          <div className="cc-field">
            <label htmlFor={passwordId}>Contraseña</label>
            <ul id={passwordRulesId} className="cc-rules-list">
              <li>Entre 8 y 72 caracteres (puedes pegar una contraseña segura)</li>
            </ul>
            <div className="cc-password-wrap">
              <input
                id={passwordId}
                name="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
                enterKeyHint="done"
                required
                minLength={8}
                maxLength={72}
                value={password}
                aria-invalid={passwordInvalid ? 'true' : 'false'}
                aria-describedby={passwordInvalid ? `${passwordRulesId} ${passwordErrorId}` : passwordRulesId}
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
                Error: la contraseña debe tener entre 8 y 72 caracteres.
              </div>
            )}
          </div>

          <button type="submit" className="cc-btn cc-btn-primary cc-btn-full" disabled={submitting}>
            {submitting ? 'Creando cuenta…' : 'Crear cuenta'}
          </button>
        </form>

        <p className="cc-auth-footer">
          ¿Ya tienes cuenta? <Link to="/login">Iniciar sesión</Link>
        </p>
      </main>
    </div>
  );
}
