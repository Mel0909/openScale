# Build

**Status: funciona.** Verificado em 2026-09-20 — `assembleDebug` gera o APK.

## Toolchain

| Peça | Versão |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose |
| AGP | 8.x (via version catalog) |
| Gradle | 9.4.1 |
| JDK | 25 (o embutido no Android Studio, em `jbr/`) |
| compileSdk / targetSdk | 37 |
| **minSdk** | **31 (Android 12)** |
| DI | Hilt |
| Persistência | Room |
| Build script | Kotlin DSL (`.gradle.kts`) + `libs.versions.toml` |

## Android Studio

Abrir a pasta **`android_app`**, não a raiz do repo.

Se a sincronização reclamar do JDK: **Settings → Build, Execution, Deployment →
Build Tools → Gradle → Gradle JDK** → escolher o JDK embutido (`jbr`).

## Linha de comando

A partir de `android_app/`:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$HOME/AppData/Local/Android/Sdk"

./gradlew assembleDebug      # APK debug
./gradlew testDebugUnitTest  # testes unitários
./gradlew lint               # lint
```

APK em `app/build/outputs/apk/debug/openScale-debug.apk`.

O APK debug é grande (~88 MB) porque Compose em debug carrega ferramentas de
inspeção. O release fica bem menor.

O primeiro build baixa Gradle, Compose e Hilt — leva vários minutos.

## Dependências

Centralizadas em `gradle/libs.versions.toml` (version catalog). Para atualizar
uma lib, é ali, não no `build.gradle.kts`.

## Build types

O upstream define variantes além de debug/release — entre elas `oss` e `beta`,
com sufixo no `versionName`. Ver `app/build.gradle.kts`.

Os keystores são procurados fora do repositório; sem eles o build segue sem
assinar.

## Fontes do design

`res/font/` tem dois `.ttf` variáveis (592 KB no total), empacotados no APK.
São parte do design — ver [design-compose.md](design-compose.md). Não
substituir por Downloadable Fonts: quebraria no F-Droid.

## Histórico

A base antiga (fork 2.3.5, Java/XML) usava AGP 4.1 e Gradle 6.5, e não abria no
Android Studio atual. Isso motivou primeiro uma migração de build e depois a
adoção do upstream 3.1.3. Ver [historico-fork.md](historico-fork.md).
