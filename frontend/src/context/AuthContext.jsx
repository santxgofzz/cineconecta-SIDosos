import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import {
  AGE_RATINGS,
  AVATAR_OPTIONS,
  createProfile as apiCreateProfile,
  getSessionState,
  loginAccount as apiLoginAccount,
  logoutAccount as apiLogoutAccount,
  registerAccount as apiRegisterAccount,
  toggleProfileActive as apiToggleProfileActive,
  updateProfile as apiUpdateProfile,
} from '../services/cineconectaApi';

const ACTIVE_PROFILE_KEY = 'cineconecta_active_profile_v1';
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [loading, setLoading] = useState(true);
  const [isDemoMode, setIsDemoMode] = useState(true);
  const [account, setAccount] = useState(null);
  const [profiles, setProfiles] = useState([]);
  const [activeProfileId, setActiveProfileId] = useState(() => {
    const saved = localStorage.getItem(ACTIVE_PROFILE_KEY);
    return saved ? Number(saved) : null;
  });

  const syncState = useCallback(async () => {
    const state = await getSessionState();
    setIsDemoMode(state.isDemoMode);
    setAccount(state.account);
    setProfiles(state.profiles);
    setActiveProfileId((prev) => {
      if (!state.account) {
        localStorage.removeItem(ACTIVE_PROFILE_KEY);
        return null;
      }
      const stillValid = state.profiles.some((p) => p.profileId === prev && p.active);
      return stillValid ? prev : null;
    });
    setLoading(false);
  }, []);

  useEffect(() => {
    syncState();
  }, [syncState]);

  const activeProfile = profiles.find((p) => p.profileId === activeProfileId && p.active) || null;

  const login = async (credentials) => {
    const state = await apiLoginAccount(credentials);
    setIsDemoMode(state.isDemoMode);
    setAccount(state.account);
    setProfiles(state.profiles);
    const firstActive = state.profiles.find((p) => p.active);
    if (firstActive && !activeProfileId) {
      setActiveProfileId(firstActive.profileId);
      localStorage.setItem(ACTIVE_PROFILE_KEY, String(firstActive.profileId));
    }
    return state;
  };

  const register = async (payload) => {
    return apiRegisterAccount(payload);
  };

  const logout = async () => {
    await apiLogoutAccount();
    localStorage.removeItem(ACTIVE_PROFILE_KEY);
    setAccount(null);
    setProfiles([]);
    setActiveProfileId(null);
  };

  const selectProfile = (profile) => {
    if (!profile?.active) return;
    setActiveProfileId(profile.profileId);
    localStorage.setItem(ACTIVE_PROFILE_KEY, String(profile.profileId));
  };

  const createProfile = async (payload) => {
    if (!account) throw new Error('Inicia sesión primero.');
    const created = await apiCreateProfile(account.accountId, payload, isDemoMode);
    await syncState();
    return created;
  };

  const updateProfile = async (profileId, payload) => {
    if (!account) throw new Error('Inicia sesión primero.');
    const updated = await apiUpdateProfile(account.accountId, profileId, payload, isDemoMode);
    await syncState();
    return updated;
  };

  const toggleProfileActive = async (profileId, activate) => {
    if (!account) throw new Error('Inicia sesión primero.');
    const updated = await apiToggleProfileActive(account.accountId, profileId, activate, isDemoMode);
    await syncState();
    return updated;
  };

  return (
    <AuthContext.Provider
      value={{
        loading,
        isDemoMode,
        account,
        profiles,
        activeProfile,
        ageRatings: AGE_RATINGS,
        avatarOptions: AVATAR_OPTIONS,
        login,
        register,
        logout,
        selectProfile,
        createProfile,
        updateProfile,
        toggleProfileActive,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider.');
  return ctx;
}
