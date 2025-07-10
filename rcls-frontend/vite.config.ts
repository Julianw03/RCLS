import path from "path"
import {defineConfig} from 'vite'
import { nodePolyfills } from 'vite-plugin-node-polyfills'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
    plugins: [
        nodePolyfills(),
        react()
    ],
    server: {
        host: '127.0.0.1',
        port: 3001
    },
    resolve: {
        alias: {
            https: 'agent-base',
            "@": path.resolve(__dirname, "./src")
        }
    }
})
