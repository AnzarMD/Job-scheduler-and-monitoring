// import { defineConfig } from 'vite'
// import react from '@vitejs/plugin-react'

// // https://vite.dev/config/
// export default defineConfig({
//   plugins: [react()],
//   define: {
//     global: 'globalThis',
//   },
// })
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  define: {
    // sockjs-client uses 'global' which doesn't exist in browsers.
    // globalThis is the standard browser equivalent.
    global: 'globalThis',
  }
})