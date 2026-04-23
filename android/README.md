# MiauGenda - App Android

App mobile para gestão de turnos da clínica veterinária.

## Stack Tecnológico

- **Linguagem:** Kotlin
- **UI:** Jetpack Compose + Material Design 3
- **Arquitetura:** MVVM (Model-View-ViewModel)
- **Networking:** Retrofit + OkHttp
- **Gestão de Estado:** StateFlow
- **Minimum SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)

## Estrutura do Projeto

```
app/src/main/kotlin/pt/ualg/miaugenda/
├── data/
│   ├── model/              # Data classes (LoginRequest, LoginResponse, User)
│   ├── remote/             # API interfaces e Retrofit client
│   └── repository/         # Camada de repositório
├── ui/
│   ├── screen/
│   │   └── login/          # Ecrã de login + ViewModel
│   └── theme/              # Tema Material Design
├── MainActivity.kt
└── MiauGendaApplication.kt
```

## Como Executar

### Pré-requisitos
- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 17
- Android SDK com API 34
- Backend a correr em `localhost:3001`

### Passos

1. **Abrir o projeto:**
   ```bash
   cd android
   # Abrir no Android Studio
   ```

2. **Sincronizar Gradle:**
   - Android Studio vai automaticamente descarregar as dependências

3. **Configurar o emulador:**
   - Criar um dispositivo virtual (AVD) no Android Studio
   - API 26+ recomendado

4. **Verificar backend:**
   - Backend deve estar a correr em `http://localhost:3001`
   - No emulador Android, `10.0.2.2:3001` redireciona para `localhost:3001` do host

5. **Executar a app:**
   - Clicar no botão Run (▶️) no Android Studio
   - Ou via terminal: `./gradlew installDebug`

## Funcionalidades Implementadas

### ✅ Ecrã de Login
- Campo para número de funcionário
- Campo para password (com toggle de visibilidade)
- Validação de campos obrigatórios
- Mensagens de erro
- Loading state durante autenticação
- Integração com API `/api/auth/login`

### ✅ Gestão de Tokens JWT
- **EncryptedSharedPreferences**: Armazenamento seguro de tokens usando AES256-GCM
- **TokenManager**: Gestão centralizada de access token, refresh token e dados do utilizador
- **AuthInterceptor**: Injeção automática do Bearer token em requisições autenticadas
- Dados guardados de forma encriptada:
  - Access Token
  - Refresh Token
  - ID do utilizador
  - Nome, email, role, categoria
  - Número de funcionário

## Credenciais de Teste

Usar os utilizadores do seed do backend:

```
Admin:
- Número: ADM001
- Password: admin123

Manager:
- Número: MGR001
- Password: manager123

Employee:
- Número: EMP001
- Password: employee123
```

## Configuração de Rede

O app está configurado para comunicar com o backend em desenvolvimento:

- **Emulador Android:** `http://10.0.2.2:3001`
- **Dispositivo físico:** Alterar `API_BASE_URL` em `app/build.gradle.kts` para o IP da máquina (ex: `http://192.168.1.X:3001`)

## Build Variants

- **debug:** Logging ativo, URL de desenvolvimento
- **release:** Logging desativado, minificação ativada

## Arquitetura de Autenticação

### Fluxo de Login
1. **User** insere credenciais no `LoginScreen`
2. **LoginViewModel** valida e chama `AuthRepository.login()`
3. **AuthRepository** faz POST para `/api/auth/login` via Retrofit
4. Se sucesso, **LoginViewModel** guarda tokens no **TokenManager**
5. **TokenManager** encripta e persiste dados em **EncryptedSharedPreferences**
6. Navegação para ecrã principal

### Autenticação de Requisições
1. **AuthInterceptor** intercepta todas as requisições HTTP
2. Se existe token e não é rota `/auth/*`, adiciona header `Authorization: Bearer {token}`
3. Backend valida o JWT e retorna dados

### Como Usar o TokenManager

```kotlin
// Obter instância
val tokenManager = MiauGendaApp.getTokenManager(context)

// Verificar se está autenticado
if (tokenManager.isLoggedIn()) {
    // User tem sessão ativa
}

// Obter dados do utilizador
val userName = tokenManager.getUserName()
val userRole = tokenManager.getUserRole()

// Fazer logout
tokenManager.clearTokens()
```

## Navegação

O app usa **Jetpack Navigation Compose** com as seguintes rotas:

- **`/login`**: Ecrã de autenticação
- **`/dashboard`**: Ecrã principal após login

### Lógica de Redirecionamento
- **App startup**: Verifica `tokenManager.isLoggedIn()`
  - Se autenticado → inicia em `/dashboard`
  - Se não autenticado → inicia em `/login`
- **Após login**: Navega para `/dashboard` removendo `/login` do backstack
- **Logout**: Limpa tokens e navega para `/login` limpando todo o backstack

## Próximos Passos

- [x] Navegação para ecrã principal após login
- [x] Armazenamento seguro de tokens (EncryptedSharedPreferences)
- [ ] Gestão de sessão e refresh tokens
- [ ] Ecrãs de gestão de turnos
- [ ] Registo de ponto
- [ ] Testes unitários e instrumentados
