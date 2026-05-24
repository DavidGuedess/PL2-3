# MiauGenda Desktop

Versão desktop da aplicação MiauGenda.

Esta app é separada da aplicação Android e consome o mesmo backend existente.  
O backend não foi alterado.

## Stack

| Camada | Tecnologia |
|---|---|
| Desktop UI | React |
| Linguagem | TypeScript |
| Build tool | Vite |
| HTTP Client | Axios |
| Navegação | React Router DOM |
| Desktop wrapper | Electron, a adicionar/usar numa fase posterior |

## Estrutura principal

```txt
desktop/
├── src/
│   ├── api/
│   ├── models/
│   ├── navigation/
│   ├── screens/
│   ├── state/
│   ├── storage/
│   ├── styles/
│   ├── App.tsx
│   └── main.tsx
├── package.json
├── tsconfig.json
└── vite.config.ts