import { useEffect, useId, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, UserCheck, UserPlus, UserX } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Navbar } from '../components/Navbar';

function ProfileEditorRow({ profile, ageRatings, avatarOptions, onSave, onToggleActive }) {
  const nameId = useId();
  const ageId = useId();
  const [name, setName] = useState(profile.name);
  const [maxAgeRating, setMaxAgeRating] = useState(String(profile.maxAgeRating));
  const [avatarUrl, setAvatarUrl] = useState(profile.avatarUrl);
  const [saving, setSaving] = useState(false);

  const handleUpdate = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await onSave(profile.profileId, { name, maxAgeRating: Number(maxAgeRating), avatarUrl });
    } finally {
      setSaving(false);
    }
  };

  return (
    <article className={`cc-manage-card ${profile.active ? '' : 'is-inactive'}`}>
      <div className="cc-manage-head">
        <img src={avatarUrl} alt={profile.name} className="cc-manage-avatar" width="64" height="64" />
        <div>
          <h3>{profile.name}</h3>
          <div className="cc-manage-badges">
            <span className={`cc-state-pill ${profile.active ? 'is-active' : 'is-off'}`}>
              {profile.active ? 'Activo' : 'Inactivo'}
            </span>
            <span className="cc-age-pill">{profile.maxAgeRating}+</span>
          </div>
        </div>
      </div>

      <form className="cc-manage-form" onSubmit={handleUpdate}>
        <div className="cc-field">
          <label htmlFor={nameId}>Nombre</label>
          <input
            id={nameId}
            type="text"
            required
            maxLength={60}
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>

        <div className="cc-field">
          <label htmlFor={ageId}>Clasificación máxima</label>
          <select id={ageId} value={maxAgeRating} onChange={(e) => setMaxAgeRating(e.target.value)}>
            {ageRatings.map((opt) => (
              <option key={opt.ageValue} value={opt.ageValue}>
                {opt.ageValue}+ · {opt.label}
              </option>
            ))}
          </select>
        </div>

        <div className="cc-avatar-picker" role="group" aria-label={`Avatar para ${profile.name}`}>
          {avatarOptions.map((av) => (
            <button
              key={av.id}
              type="button"
              className={`cc-mini-avatar-btn ${avatarUrl === av.src ? 'is-selected' : ''}`}
              aria-label={`Elegir avatar ${av.label}`}
              aria-pressed={avatarUrl === av.src}
              onClick={() => setAvatarUrl(av.src)}
            >
              <img src={av.src} alt="" width="40" height="40" />
            </button>
          ))}
        </div>

        <div className="cc-manage-actions">
          <button type="submit" className="cc-btn cc-btn-primary cc-btn-compact" disabled={saving}>
            <Save size={15} aria-hidden="true" />
            <span>Guardar cambios</span>
          </button>
          <button
            type="button"
            className="cc-btn cc-btn-ghost cc-btn-compact"
            onClick={() => onToggleActive(profile.profileId, !profile.active)}
          >
            {profile.active ? (
              <>
                <UserX size={15} aria-hidden="true" />
                <span>Desactivar</span>
              </>
            ) : (
              <>
                <UserCheck size={15} aria-hidden="true" />
                <span>Reactivar</span>
              </>
            )}
          </button>
        </div>
      </form>
    </article>
  );
}

export function ProfileManagePage() {
  const { loading, account, profiles, ageRatings, avatarOptions, createProfile, updateProfile, toggleProfileActive } =
    useAuth();
  const navigate = useNavigate();

  const newNameId = useId();
  const newAgeId = useId();

  const [newName, setNewName] = useState('');
  const [newAgeRating, setNewAgeRating] = useState('12');
  const [newAvatarUrl, setNewAvatarUrl] = useState(avatarOptions[0].src);
  const [feedback, setFeedback] = useState(null);
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    if (!loading && !account) {
      navigate('/login', { replace: true });
    }
  }, [loading, account, navigate]);

  if (loading || !account) return null;

  const activeCount = profiles.filter((p) => p.active).length;

  const handleCreate = async (e) => {
    e.preventDefault();
    setFeedback(null);
    if (!newName.trim()) {
      setFeedback({ type: 'error', text: 'Error: ingresa un nombre para el perfil.' });
      return;
    }
    setCreating(true);
    try {
      await createProfile({ name: newName.trim(), maxAgeRating: Number(newAgeRating), avatarUrl: newAvatarUrl });
      setNewName('');
      setFeedback({ type: 'success', text: 'Perfil creado correctamente.' });
    } catch (err) {
      setFeedback({ type: 'error', text: `Error: ${err.message}` });
    } finally {
      setCreating(false);
    }
  };

  const handleSave = async (profileId, payload) => {
    setFeedback(null);
    try {
      await updateProfile(profileId, payload);
      setFeedback({ type: 'success', text: 'Cambios guardados correctamente.' });
    } catch (err) {
      setFeedback({ type: 'error', text: `Error: ${err.message}` });
    }
  };

  const handleToggle = async (profileId, activate) => {
    setFeedback(null);
    try {
      await toggleProfileActive(profileId, activate);
      setFeedback({
        type: 'success',
        text: activate ? 'Perfil reactivado correctamente.' : 'Perfil desactivado (cupo liberado).',
      });
    } catch (err) {
      setFeedback({ type: 'error', text: `Error: ${err.message}` });
    }
  };

  return (
    <>
      <Navbar />
      <main className="cc-main cc-container cc-manage-main">
        <div className="cc-manage-topbar">
          <div>
            <h1>Administrar perfiles</h1>
            <p className="cc-profiles-sub">
              Plan <strong>{account.planName}</strong> · Cupo utilizado:{' '}
              <strong>
                {activeCount} / {account.maxProfiles} perfiles activos
              </strong>
            </p>
          </div>
          <Link to="/profiles" className="cc-btn cc-btn-ghost">
            <ArrowLeft size={16} aria-hidden="true" />
            <span>Volver a selección</span>
          </Link>
        </div>

        {feedback && (
          <p
            className={`cc-alert ${feedback.type === 'error' ? 'cc-alert-error' : 'cc-alert-success'}`}
            role={feedback.type === 'error' ? 'alert' : 'status'}
          >
            {feedback.text}
          </p>
        )}

        <section className="cc-manage-create">
          <h2>Crear nuevo perfil</h2>
          <form className="cc-create-grid" onSubmit={handleCreate}>
            <div className="cc-field">
              <label htmlFor={newNameId}>Nombre del perfil</label>
              <input
                id={newNameId}
                type="text"
                required
                maxLength={60}
                placeholder="Ej. Infantil"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
              />
            </div>

            <div className="cc-field">
              <label htmlFor={newAgeId}>Clasificación máxima</label>
              <select id={newAgeId} value={newAgeRating} onChange={(e) => setNewAgeRating(e.target.value)}>
                {ageRatings.map((opt) => (
                  <option key={opt.ageValue} value={opt.ageValue}>
                    {opt.ageValue}+ · {opt.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="cc-field">
              <span>Avatar</span>
              <div className="cc-avatar-picker" role="group" aria-label="Elegir avatar para nuevo perfil">
                {avatarOptions.map((av) => (
                  <button
                    key={av.id}
                    type="button"
                    className={`cc-mini-avatar-btn ${newAvatarUrl === av.src ? 'is-selected' : ''}`}
                    aria-label={`Avatar ${av.label}`}
                    aria-pressed={newAvatarUrl === av.src}
                    onClick={() => setNewAvatarUrl(av.src)}
                  >
                    <img src={av.src} alt="" width="40" height="40" />
                  </button>
                ))}
              </div>
            </div>

            <div className="cc-create-submit">
              <button type="submit" className="cc-btn cc-btn-primary" disabled={creating}>
                <UserPlus size={16} aria-hidden="true" />
                <span>{creating ? 'Creando…' : 'Crear perfil'}</span>
              </button>
            </div>
          </form>
        </section>

        <section className="cc-section" aria-label="Perfiles de la cuenta">
          <h2 className="cc-section-title">Tus perfiles ({profiles.length})</h2>
          <div className="cc-manage-grid">
            {profiles.map((profile) => (
              <ProfileEditorRow
                key={profile.profileId}
                profile={profile}
                ageRatings={ageRatings}
                avatarOptions={avatarOptions}
                onSave={handleSave}
                onToggleActive={handleToggle}
              />
            ))}
          </div>
        </section>
      </main>
    </>
  );
}
