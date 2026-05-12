import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export const useAuthStore = create(
  persist(
    (set) => ({
      token: null,
      user: null,

      // Called after successful login — stores token and user info
      login: (token, user) => {
        localStorage.setItem('cloudflow_token', token);
        set({ token, user });
      },

      // Called on logout — clears everything
      logout: () => {
        localStorage.removeItem('cloudflow_token');
        localStorage.removeItem('cloudflow_user');
        set({ token: null, user: null });
      },

      isAuthenticated: () => {
        const state = useAuthStore.getState();
        return !!state.token;
      }
    }),
    {
      name: 'cloudflow-auth', // localStorage key
      // Only persist token and user — not functions
      partialize: (state) => ({ token: state.token, user: state.user })
    }
  )
);