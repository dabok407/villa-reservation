import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [tailwindcss()],
  base: '/villa/',
  server: {
    proxy: {
      '/villa/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
