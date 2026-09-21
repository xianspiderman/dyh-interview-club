import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig({plugins:[vue()],server:{port:5173,proxy:{'/api':{target:process.env.CLUB_API_URL || 'http://localhost:3020',changeOrigin:true},'/actuator':{target:process.env.CLUB_API_URL || 'http://localhost:3020',changeOrigin:true}}}})
