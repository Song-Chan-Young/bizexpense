import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 개발 중에는 /api 요청을 Spring Boot(8080)로 넘겨 CORS 없이 사용한다.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
