import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import axios from 'axios';
import authService, { AuthResponse, User } from '../services/authService';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  setTokens: (tokens: AuthResponse) => void;
  setUser: (user: User) => void;
  logout: () => void;
  clearError: () => void;
  loadUser: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      setTokens: (tokens: AuthResponse) => {
        set({
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
          isAuthenticated: true,
        });

        // Set default authorization header
        axios.defaults.headers.common['Authorization'] = `Bearer ${tokens.accessToken}`;
      },

      setUser: (user: User) => {
        set({ user });
      },

      logout: async () => {
        const { refreshToken } = get();

        try {
          if (refreshToken) {
            await authService.logout(refreshToken);
          }
        } catch (error) {
          console.error('Logout error:', error);
        } finally {
          set({
            accessToken: null,
            refreshToken: null,
            user: null,
            isAuthenticated: false,
          });
          delete axios.defaults.headers.common['Authorization'];
        }
      },

      clearError: () => set({ error: null }),

      loadUser: async () => {
        const { accessToken } = get();

        if (!accessToken) return;

        set({ isLoading: true, error: null });

        try {
          const user = await authService.getCurrentUser();
          set({ user, isLoading: false });
        } catch (error: any) {
          set({
            error: error.response?.data?.message || 'Failed to load user',
            isLoading: false
          });
        }
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
      }),
      onRehydrateStorage: () => (state) => {
        if (state?.accessToken) {
          axios.defaults.headers.common['Authorization'] = `Bearer ${state.accessToken}`;
        }
      },
    }
  )
);

// Axios interceptor for automatic token refresh
axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const { refreshToken, setTokens, logout } = useAuthStore.getState();

      if (refreshToken) {
        try {
          const tokens = await authService.refreshToken(refreshToken);
          setTokens(tokens);
          originalRequest.headers['Authorization'] = `Bearer ${tokens.accessToken}`;
          return axios(originalRequest);
        } catch (refreshError) {
          logout();
          return Promise.reject(refreshError);
        }
      } else {
        logout();
      }
    }

    return Promise.reject(error);
  }
);
