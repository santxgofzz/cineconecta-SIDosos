const DB_KEY = 'cineconecta_demo_db_v1';
const SESSION_KEY = 'cineconecta_demo_session_v1';
const AVATAR_KEY = 'cineconecta_profile_avatars_v1';

export const AVATAR_OPTIONS = [
  { id: 'principal', label: 'Aventurero', src: '/images/avatar-principal.jpg' },
  { id: 'ninos', label: 'Infantil', src: '/images/avatar-ninos.jpg' },
  { id: 'cineasta', label: 'Cineasta', src: '/images/avatar-cineasta.jpg' },
  { id: 'invitado', label: 'Cósmico', src: '/images/avatar-invitado.jpg' },
];

export const AGE_RATINGS = [
  { ageValue: 0, label: 'Todo público' },
  { ageValue: 7, label: 'Mayores de 7 años' },
  { ageValue: 12, label: 'Mayores de 12 años' },
  { ageValue: 15, label: 'Mayores de 15 años' },
  { ageValue: 18, label: 'Mayores de 18 años' },
];

function ageLabel(ageValue) {
  return AGE_RATINGS.find((r) => r.ageValue === Number(ageValue))?.label ?? `Mayores de ${ageValue} años`;
}

function defaultAvatarFor(profile) {
  if (profile.maxAgeRating <= 7) return '/images/avatar-ninos.jpg';
  if (!profile.active) return '/images/avatar-invitado.jpg';
  return '/images/avatar-principal.jpg';
}

function loadAvatarMap() {
  try {
    return JSON.parse(localStorage.getItem(AVATAR_KEY) || '{}');
  } catch {
    return {};
  }
}

export function getProfileAvatar(profile) {
  const map = loadAvatarMap();
  return map[String(profile.profileId)] || profile.avatarUrl || defaultAvatarFor(profile);
}

export function setProfileAvatar(profileId, avatarSrc) {
  const map = loadAvatarMap();
  map[String(profileId)] = avatarSrc;
  localStorage.setItem(AVATAR_KEY, JSON.stringify(map));
}

function createSeedDb() {
  return {
    nextAccountId: 3,
    nextProfileId: 100,
    accounts: [
      {
        accountId: 1,
        fullName: 'Cuenta Demo Familiar',
        email: 'familia@ejemplo.invalid',
        password: 'SemillaLocal2026!',
        planName: 'Familiar',
        maxProfiles: 3,
        createdAt: '2026-09-15T10:00:00Z',
      },
    ],
    profiles: [
      {
        profileId: 15,
        accountId: 1,
        name: 'Principal',
        maxAgeRating: 18,
        ageRatingLabel: 'Mayores de 18 años',
        active: true,
        stateCode: 'A',
        avatarUrl: '/images/avatar-principal.jpg',
      },
      {
        profileId: 16,
        accountId: 1,
        name: 'Niños',
        maxAgeRating: 7,
        ageRatingLabel: 'Mayores de 7 años',
        active: true,
        stateCode: 'A',
        avatarUrl: '/images/avatar-ninos.jpg',
      },
      {
        profileId: 17,
        accountId: 1,
        name: 'Invitado',
        maxAgeRating: 12,
        ageRatingLabel: 'Mayores de 12 años',
        active: false,
        stateCode: 'I',
        avatarUrl: '/images/avatar-invitado.jpg',
      },
    ],
  };
}

function loadDb() {
  try {
    const raw = localStorage.getItem(DB_KEY);
    if (raw) return JSON.parse(raw);
  } catch {
    // Reiniciar semilla si el almacenamiento local está dañado
  }
  const seed = createSeedDb();
  localStorage.setItem(DB_KEY, JSON.stringify(seed));
  return seed;
}

function saveDb(db) {
  localStorage.setItem(DB_KEY, JSON.stringify(db));
}

function getDemoSessionAccountId() {
  const raw = localStorage.getItem(SESSION_KEY);
  return raw ? Number(raw) : null;
}

async function tryRealJson(url, options = {}) {
  try {
    const res = await fetch(url, {
      credentials: 'include',
      headers: { Accept: 'application/json', ...(options.headers || {}) },
      ...options,
    });
    const contentType = res.headers.get('content-type') || '';
    if (res.status >= 500 && !contentType.includes('application/json')) {
      return { unreachable: true };
    }
    if (res.redirected && res.url.includes('/login')) {
      return { unreachable: false, unauthenticated: true };
    }
    if (!res.ok) {
      const errBody = contentType.includes('application/json') ? await res.json().catch(() => null) : null;
      return { unreachable: false, error: errBody?.message || `Error ${res.status}` };
    }
    if (res.status === 204) {
      return { unreachable: false, data: null };
    }
    const data = contentType.includes('application/json') ? await res.json() : null;
    return { unreachable: false, data };
  } catch {
    return { unreachable: true };
  }
}

export async function getSessionState() {
  const demoId = getDemoSessionAccountId();
  if (demoId != null) {
    const db = loadDb();
    const acc = db.accounts.find((a) => a.accountId === demoId);
    if (acc) {
      return {
        isDemoMode: true,
        account: {
          accountId: acc.accountId,
          fullName: acc.fullName,
          email: acc.email,
          planName: acc.planName,
          maxProfiles: acc.maxProfiles,
        },
        profiles: db.profiles
          .filter((p) => p.accountId === acc.accountId)
          .map((p) => ({ ...p, avatarUrl: getProfileAvatar(p) })),
      };
    }
  }

  const realAcc = await tryRealJson('/api/accounts/me');
  if (!realAcc.unreachable && !realAcc.unauthenticated && realAcc.data) {
    const realProfiles = await tryRealJson('/api/profiles');
    return {
      isDemoMode: false,
      account: { ...realAcc.data, planName: 'Suscripción activa', maxProfiles: 5 },
      profiles: (realProfiles.data || []).map((p) => ({ ...p, avatarUrl: getProfileAvatar(p) })),
    };
  }

  return { isDemoMode: true, account: null, profiles: [] };
}

export async function registerAccount({ fullName, email, password }) {
  const cleanName = fullName.trim();
  const cleanEmail = email.trim().toLowerCase();

  // Intentar backend real primero si estuviera levantado (con cookie CSRF si existe)
  const csrfCookie = document.cookie
    .split('; ')
    .find((c) => c.startsWith('XSRF-TOKEN='))
    ?.split('=')[1];

  if (csrfCookie) {
    const real = await tryRealJson('/api/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': decodeURIComponent(csrfCookie),
      },
      body: JSON.stringify({ fullName: cleanName, email: cleanEmail, password }),
    });
    if (!real.unreachable) {
      if (real.error) throw new Error(real.error);
      return { isDemoMode: false, account: real.data };
    }
  }

  const db = loadDb();
  if (db.accounts.some((a) => a.email.toLowerCase() === cleanEmail)) {
    throw new Error('Ya existe una cuenta registrada con ese correo.');
  }
  const newAcc = {
    accountId: db.nextAccountId++,
    fullName: cleanName,
    email: cleanEmail,
    password,
    planName: 'Familiar',
    maxProfiles: 3,
    createdAt: new Date().toISOString(),
  };
  db.accounts.push(newAcc);
  db.profiles.push({
    profileId: db.nextProfileId++,
    accountId: newAcc.accountId,
    name: cleanName.split(' ')[0] || 'Principal',
    maxAgeRating: 18,
    ageRatingLabel: ageLabel(18),
    active: true,
    stateCode: 'A',
    avatarUrl: '/images/avatar-principal.jpg',
  });
  saveDb(db);
  return { isDemoMode: true, account: newAcc };
}

export async function loginAccount({ email, password }) {
  const cleanEmail = email.trim().toLowerCase();
  const db = loadDb();
  const demoMatch = db.accounts.find((a) => a.email.toLowerCase() === cleanEmail);

  if (demoMatch) {
    if (demoMatch.password !== password) {
      throw new Error('Correo o contraseña incorrectos.');
    }
    localStorage.setItem(SESSION_KEY, String(demoMatch.accountId));
    return getSessionState();
  }

  // Fallback a Spring Security si estuviera levantado
  try {
    const form = new URLSearchParams({ username: cleanEmail, password });
    const res = await fetch('/login', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: form,
    });
    if (res.ok) {
      localStorage.removeItem(SESSION_KEY);
      return getSessionState();
    }
  } catch {
    // Backend no levantado
  }

  throw new Error('Correo o contraseña incorrectos.');
}

export async function logoutAccount() {
  localStorage.removeItem(SESSION_KEY);
  try {
    await fetch('/logout', { method: 'POST', credentials: 'include' });
  } catch {
    // Ignorar si estamos en modo demo
  }
}

export async function createProfile(accountId, { name, maxAgeRating, avatarUrl }, isDemoMode) {
  if (!isDemoMode) {
    const real = await tryRealJson('/api/profiles', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: name.trim(), maxAgeRating: Number(maxAgeRating) }),
    });
    if (!real.unreachable) {
      if (real.error) throw new Error(real.error);
      if (avatarUrl) setProfileAvatar(real.data.profileId, avatarUrl);
      return { ...real.data, avatarUrl: avatarUrl || getProfileAvatar(real.data) };
    }
  }

  const db = loadDb();
  const acc = db.accounts.find((a) => a.accountId === accountId);
  if (!acc) throw new Error('Cuenta no encontrada.');
  const cleanName = name.trim();
  const rating = Number(maxAgeRating);
  const activeCount = db.profiles.filter((p) => p.accountId === accountId && p.active).length;

  if (activeCount >= acc.maxProfiles) {
    throw new Error(`Se alcanzó el máximo de ${acc.maxProfiles} perfiles activos permitidos por tu plan ${acc.planName}.`);
  }
  if (db.profiles.some((p) => p.accountId === accountId && p.name.toLowerCase() === cleanName.toLowerCase())) {
    throw new Error('Ya existe un perfil con ese nombre en tu cuenta.');
  }

  const created = {
    profileId: db.nextProfileId++,
    accountId,
    name: cleanName,
    maxAgeRating: rating,
    ageRatingLabel: ageLabel(rating),
    active: true,
    stateCode: 'A',
    avatarUrl: avatarUrl || (rating <= 7 ? '/images/avatar-ninos.jpg' : '/images/avatar-principal.jpg'),
  };
  db.profiles.push(created);
  saveDb(db);
  setProfileAvatar(created.profileId, created.avatarUrl);
  return created;
}

export async function updateProfile(accountId, profileId, { name, maxAgeRating, avatarUrl }, isDemoMode) {
  if (!isDemoMode) {
    const real = await tryRealJson(`/api/profiles/${profileId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: name.trim(), maxAgeRating: Number(maxAgeRating) }),
    });
    if (!real.unreachable) {
      if (real.error) throw new Error(real.error);
      if (avatarUrl) setProfileAvatar(profileId, avatarUrl);
      return { ...real.data, avatarUrl: avatarUrl || getProfileAvatar(real.data) };
    }
  }

  const db = loadDb();
  const profile = db.profiles.find((p) => p.profileId === profileId && p.accountId === accountId);
  if (!profile) throw new Error('El perfil no pertenece a tu cuenta.');
  const cleanName = name.trim();
  const rating = Number(maxAgeRating);

  if (
    db.profiles.some(
      (p) => p.accountId === accountId && p.profileId !== profileId && p.name.toLowerCase() === cleanName.toLowerCase()
    )
  ) {
    throw new Error('Ya existe otro perfil con ese nombre en tu cuenta.');
  }

  profile.name = cleanName;
  profile.maxAgeRating = rating;
  profile.ageRatingLabel = ageLabel(rating);
  if (avatarUrl) {
    profile.avatarUrl = avatarUrl;
    setProfileAvatar(profileId, avatarUrl);
  }
  saveDb(db);
  return { ...profile };
}

export async function toggleProfileActive(accountId, profileId, activate, isDemoMode) {
  const action = activate ? 'reactivate' : 'deactivate';
  if (!isDemoMode) {
    const real = await tryRealJson(`/api/profiles/${profileId}/${action}`, { method: 'POST' });
    if (!real.unreachable) {
      if (real.error) throw new Error(real.error);
      return { ...real.data, avatarUrl: getProfileAvatar(real.data) };
    }
  }

  const db = loadDb();
  const acc = db.accounts.find((a) => a.accountId === accountId);
  const profile = db.profiles.find((p) => p.profileId === profileId && p.accountId === accountId);
  if (!profile || !acc) throw new Error('El perfil no pertenece a tu cuenta.');

  if (activate && !profile.active) {
    const activeCount = db.profiles.filter((p) => p.accountId === accountId && p.active).length;
    if (activeCount >= acc.maxProfiles) {
      throw new Error(`No puedes reactivar: tu plan ${acc.planName} permite máximo ${acc.maxProfiles} perfiles activos.`);
    }
  }

  profile.active = activate;
  profile.stateCode = activate ? 'A' : 'I';
  saveDb(db);
  return { ...profile };
}
