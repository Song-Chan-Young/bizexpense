import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 같은 Wi-Fi 의 폰에서도 http://<PC IP>:5173 으로 접속해 모바일 화면을 확인한다.
    host: true,
    // 개발 중에는 /api 요청을 Spring Boot(8080)로 넘겨 CORS 없이 사용한다.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
