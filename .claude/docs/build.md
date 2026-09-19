# Build e verificação

## Estado do ambiente local (verificado em 2026-09-19)

| Requisito | Necessário | Nesta máquina |
|---|---|---|
| JDK | 8–11 (exigência de Gradle 6.5 + AGP 4.1) | **JDK 25 (Temurin)** ❌ |
| Android SDK | API 29 + build-tools | **não detectado** (`ANDROID_HOME` vazio) ❌ |
| Gradle | 6.5 (via wrapper, baixa sozinho) | wrapper presente ✔ |

**Conclusão: `./gradlew` não roda nesta máquina sem configuração adicional.**
Gradle 6.5 falha com JDK acima de 15 (erro típico de acesso reflexivo a
`java.lang.reflect` / classes internas do JDK).

### Para habilitar o build

Uma das opções:

1. **Android Studio** — traz JDK embutido e gerencia o SDK. Caminho mais simples.
2. **JDK 11 paralelo** — instalar e apontar:
   ```
   android_app/gradle.properties:
   org.gradle.java.home=C:\\Program Files\\Eclipse Adoptium\\jdk-11...
   ```
   Mais o SDK do Android em `android_app/local.properties`:
   ```
   sdk.dir=C\:\\Users\\melbi\\AppData\\Local\\Android\\Sdk
   ```
   Os dois arquivos estão no `.gitignore` — são locais, não entram em commit.

## Comandos (quando o ambiente estiver pronto)

Sempre a partir de `android_app/`:

```bash
./gradlew assembleDebug          # APK debug
./gradlew test                   # testes unitários JVM (rápidos, sem device)
./gradlew connectedAndroidTest   # testes Espresso (precisa device/emulador)
./gradlew lint                   # lint (abortOnError false — não quebra o build)
```

APK sai em `android_app/app/build/outputs/apk/`.

## Como verificar mudanças de UI sem conseguir compilar

Enquanto o build local não estiver disponível, mudanças visuais precisam ser
verificadas por leitura. Checklist:

1. **Recursos referenciados existem?**
   Toda cor/drawable/string nova precisa estar declarada. `@color/foo` inexistente
   só falha na compilação de recursos.
   ```bash
   grep -rn "@color/" android_app/app/src/main/res android_app/app/src/main/java | \
     grep -o "@color/[a-zA-Z_]*" | sort -u
   ```
   Comparar com o que existe em `res/values/colors.xml`.

2. **Ids usados por `findViewById` continuam existindo?**
   Sem ViewBinding, renomear um id no XML quebra só em runtime (NPE).

3. **Testes Espresso ainda batem?**
   `androidTest/.../gui/` referencia ids e textos.

4. **Não sobrou literal de cor** onde se pretendia usar o tema.

## CI

`.travis.yml` (legado, provavelmente inativo) compilava debug + release e
rodava os testes unitários. Não há GitHub Actions configurado — `.github/` só
tem templates de issue.

## Build types

| Type | applicationId | Diferença |
|---|---|---|
| `debug` | `com.health.openscale` | — |
| `release` | `com.health.openscale` | assinado, proguard (minify off) |
| `light` | `...light` | ícone próprio, sem item de doação no drawer |
| `pro` | `...pro` | ícone próprio, sem item de doação no drawer |

As diferenças são só de ícone/assinatura/menu — **não há divergência de código de UI**
entre os flavors. Uma mudança visual vale para os quatro.

Os keystores são procurados **fora do repositório** (`../../openScale.keystore`);
se não existirem, o build segue sem assinar.
