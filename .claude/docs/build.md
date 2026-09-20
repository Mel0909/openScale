# Build

**Status: funciona.** Verificado em 2026-09-20 — `assembleDebug` gera o APK e
`testDebugUnitTest` passa.

## Toolchain

| Peça | Versão |
|---|---|
| AGP | 8.13.0 |
| Gradle | 9.1.0 |
| JDK | 25 (o embutido no Android Studio, em `jbr/`) |
| compileSdk | 35 |
| minSdk / targetSdk | 21 / 29 |

`targetSdk` segue em 29 **de propósito**: subir para 31+ passa a exigir
`BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` e mexeria na camada de balanças, que não se
altera sem hardware para testar. Ver
[balancas-bluetooth.md](balancas-bluetooth.md).

## Android Studio

Abrir a pasta **`android_app`**, não a raiz do repo.

Se a sincronização reclamar do JDK: **Settings → Build, Execution, Deployment →
Build Tools → Gradle → Gradle JDK** → escolher o JDK embutido (`jbr`).

## Linha de comando

A partir de `android_app/`, com as duas variáveis apontando para o que o Android
Studio instalou:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$HOME/AppData/Local/Android/Sdk"

./gradlew assembleDebug          # APK debug
./gradlew testDebugUnitTest      # testes unitários JVM
./gradlew connectedAndroidTest   # Espresso (precisa de device)
./gradlew lint                   # lint (abortOnError false)
```

APK em `app/build/outputs/apk/debug/app-debug.apk`.

O primeiro build baixa o Gradle 9.1 e as dependências — leva vários minutos.

## Armadilhas do AGP 8 que já custaram caro

Registradas porque voltam a morder em qualquer mudança futura:

- **`switch` sobre `R.id` não compila.** Os campos de `R` deixaram de ser `final`,
  e `case` exige constante. Use `if/else`. Foram 10 ocorrências em 5 arquivos.
- **Estilos com ponto herdam implicitamente do prefixo.**
  `Widget.OpenScale.LabelMicro` procura `Widget.OpenScale`; se ele não existe,
  o link de recursos falha. Declare `parent=""` para cortar a herança.
- **`buildConfig true` é obrigatório** se o código usa `BuildConfig` — deixou de
  ser gerado por padrão.
- **`namespace` vive no `build.gradle`**, não mais no `package` do manifest.
- **`android:exported` explícito** em todo componente com `intent-filter`.
- **Build types não-padrão** (`light`, `pro`) precisam de `matchingFallbacks`.

## Build types

| Type | applicationId | Diferença |
|---|---|---|
| `debug` | `com.health.openscale` | — |
| `release` | `com.health.openscale` | assinado |
| `light` | `...light` | ícone próprio, sem doação no menu |
| `pro` | `...pro` | ícone próprio, sem doação no menu |

Os keystores são procurados **fora do repositório** (`../../openScale.keystore`);
sem eles o build segue sem assinar.

## CI

`.travis.yml` é legado e aponta para a toolchain antiga — se for reativado,
precisa ser atualizado junto.
