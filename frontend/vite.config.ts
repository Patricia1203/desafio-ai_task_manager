import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Porta fixa: se outra instancia estiver ativa, o vite falha em vez de
    // subir em 5174 — outra origem nao esta no app.cors.allowed-origins e o
    // backend responde "Invalid CORS request" (nao-JSON) para a UI.
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
});