---
description: Compila o app e roda os testes, reportando só o que falhou
---

Compile o projeto e rode os testes unitários, a partir de `android_app/`:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$HOME/AppData/Local/Android/Sdk"
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

O primeiro build de uma sessão pode levar vários minutos — rode em background
e aguarde, não interrompa.

Reporte:
- se compilou e se os testes passaram;
- para cada erro, o arquivo, a linha e a causa provável;
- se falhou, proponha a correção antes de aplicá-la.

Se passar tudo, diga isso de forma direta, com o caminho do APK. Sem inventar
ressalvas.
