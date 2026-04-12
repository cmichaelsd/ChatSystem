/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        mono: ['"JetBrains Mono"', '"Fira Code"', 'ui-monospace', 'monospace'],
      },
      colors: {
        surface: {
          base: '#0f1117',
          panel: '#161b22',
          elevated: '#1c2128',
          border: '#30363d',
        },
        text: {
          primary: '#e6edf3',
          muted: '#7d8590',
        },
        accent: {
          highlight: '#f0883e',
          online: '#3fb950',
          danger: '#f85149',
        },
      },
    },
  },
  plugins: [],
}
