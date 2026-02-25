export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#f0f9f0', 100: '#dcf0dc', 200: '#bbe2bb', 300: '#8ecc8e',
          400: '#5cb05c', 500: '#3a9e3a', 600: '#2d7e2d', 700: '#256325',
          800: '#1f4f1f', 900: '#1a401a',
        },
      },
      fontFamily: {
        sans: ['"DM Sans"', 'sans-serif'],
        display: ['"Syne"', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
