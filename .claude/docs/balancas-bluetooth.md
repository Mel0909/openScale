# Balanças Bluetooth — como funciona e o que nunca quebrar

Documento de segurança para a reconstrução da UI. A camada Bluetooth é a parte do
app que **falha silenciosamente**: um erro aqui não dá crash nem erro de compilação —
a balança simplesmente para de conectar, ou conecta e não entrega dado, e só se
descobre com hardware real na mão.

**Regra geral: não edite nada em `core/bluetooth/`.** O que a UI nova precisa é
conversar certo com essa camada. Este documento explica como.

---

## 1. O caminho completo de uma pesagem

```
usuário toca "Pesar agora"
   ↓
MainActivity.invokeConnectToBluetoothDevice()        ← validações (§4)
   ↓
OpenScale.connectToBluetoothDevice(name, mac, handler)
   ↓
BluetoothFactory.createDeviceDriver(context, name)   ← escolhe driver pelo NOME
   ↓
driver.registerCallbackHandler(handler)              ← OBRIGATÓRIO antes de connect
driver.connect(mac)
   ↓
[ scan LE → conecta → descobre serviços → máquina de estados §2 ]
   ↓
driver.addScaleMeasurement(medição)
   ↓
Handler.handleMessage(BT_STATUS.RETRIEVE_SCALE_DATA) ← na UI (§3)
   ↓
merge com última medição (se preference ligada)
   ↓
OpenScale.addScaleMeasurement(dados, silent=true)    ← pipeline de regras §5
   ↓
Room insere → LiveData dispara → telas atualizam
```

A UI aparece em **dois pontos**: dispara a conexão e recebe o resultado por `Handler`.
Todo o resto é `core/`.

---

## 2. A máquina de estados dos drivers

`BluetoothCommunication` (`core/bluetooth/BluetoothCommunication.java`) é a base
dos 24 drivers. Cada driver implementa `onNextStep(int stepNr)` como um `switch`
que executa um passo de inicialização por vez.

```java
protected boolean onNextStep(int stepNr) {
    switch (stepNr) {
        case 0: writeBytes(...); break;          // avança sozinho
        case 1: setNotificationOn(...); break;   // PARA e espera callback
        case 2: stopMachineState(); break;       // PARA até resumeMachineState()
        default: return false;                   // fim → desconecta em 60s
    }
    return true;
}
```

### As regras não óbvias

| Operação | Comportamento |
|---|---|
| `writeBytes()` | Avança sozinho — `onCharacteristicWrite` chama `nextMachineStep()`. |
| `setNotificationOn()` / `setIndicationOn()` | Chamam `stopMachineState()` internamente. Retomam em `onNotificationStateUpdate`. **Não chame `resumeMachineState()` manualmente depois.** |
| `readBytes()` | **Não avança sozinho.** O comentário no código avisa: `nextMachineStep()` precisa ser chamado manualmente. |
| `stopMachineState()` | Congela a máquina. Só `resumeMachineState()` destrava. Esquecer = trava para sempre. |
| `return false` | Encerra a inicialização e agenda desconexão em 60s. |

**Timeout global de 60s**: `resetDisconnectTimer()` é chamado a cada notificação
recebida. Se a balança ficar 60s sem enviar nada, desconecta. Para uma balança que
espera o usuário subir nela, é isso que dá a janela de espera.

`onBluetoothNotify(uuid, bytes)` é onde cada driver faz o parse binário do seu
protocolo. É o código mais sensível do repositório — bytes crus, checksums,
endianness, escalas de unidade.

---

## 3. O contrato com a UI: `BT_STATUS`

O driver fala com a UI por `Handler`, usando `msg.what` = ordinal do enum.

```java
public enum BT_STATUS {
    RETRIEVE_SCALE_DATA,      // msg.obj = ScaleMeasurement  ← o dado chegou
    INIT_PROCESS,
    CONNECTION_RETRYING,
    CONNECTION_ESTABLISHED,
    CONNECTION_DISCONNECT,
    CONNECTION_LOST,
    NO_DEVICE_FOUND,
    UNEXPECTED_ERROR,         // msg.obj = texto do erro
    SCALE_MESSAGE             // msg.arg1 = string res id, msg.obj = valor
}
```

### ⚠️ A ordem do enum é o protocolo

`BT_STATUS.values()[msg.what]` converte o inteiro de volta em estado.
**Inserir um valor no meio do enum quebra todos os drivers de uma vez**, sem erro
de compilação. Se precisar de um estado novo, **acrescente no fim**.

### O que a UI nova precisa implementar

Um `Handler` que trate os 9 casos. Hoje isso vive em
`MainActivity.callbackBtHandler` e faz duas coisas:

1. **Atualiza o ícone de status** na toolbar (buscando / conectado / perdido).
   O design novo não tem toolbar — **precisa decidir onde esse estado aparece**.
   Sugestão natural: dentro do sheet "Pesar agora", que já tem o estado
   "Aguardando a balança…".
2. **Trata `RETRIEVE_SCALE_DATA`**, que contém a regra de merge (§5).

`SCALE_MESSAGE` carrega um **string resource id** em `msg.arg1`, resolvido com
`getResources().getString(msg.arg1)`. Mensagens da própria balança (ex.: "pesagem
fora de faixa"). Mantenha essas strings.

---

## 4. Validações antes de conectar — todas obrigatórias

`invokeConnectToBluetoothDevice()` faz esta sequência. A UI nova **precisa repetir
todas**, ou a conexão falha de formas confusas:

1. **Build `light`** → mostra diálogo de upgrade e retorna (não conecta).
2. **Usuário selecionado?** Se `getSelectedScaleUserId() == -1`, avisa e retorna.
3. **MAC válido?** `BluetoothAdapter.checkBluetoothAddress(hwAddress)`.
   Sem balança pareada → "nenhum dispositivo configurado".
4. **Bluetooth ligado?** Se não, dispara `ACTION_REQUEST_ENABLE`.
5. **Driver existe?** `connectToBluetoothDevice()` retorna `false` se
   `createDeviceDriver()` não reconhecer o nome → "balança não suportada".

### Permissão de localização — o detalhe que mais confunde

Android exige `ACCESS_FINE_LOCATION` para escanear BLE. Além da permissão, o
**serviço de localização precisa estar ligado** (`PermissionHelper.requestLocationServicePermission`).

E tem um comportamento deliberado em `connect()`:

```java
// Running an LE scan during connect improves connectivity on some phones
// (e.g. Sony Xperia Z5 compact). For some scales (e.g. Medisana BS444)
// it seems to be a requirement that the scale is discovered before connecting.
```

Com permissão → faz scan antes de conectar (e chama `stopMachineState()`).
Sem permissão → conecta direto, **e algumas balanças nunca conectam assim**.
Se um usuário relatar "não conecta", localização desligada é a primeira suspeita.

> **Nota para quando o targetSdk subir:** a partir do Android 12 (API 31) existem
> `BLUETOOTH_SCAN` e `BLUETOOTH_CONNECT`. O app hoje é `targetSdk 29` e usa o
> modelo antigo. Isso vai precisar de atenção numa modernização do build — mas
> **não mexa nisso junto com a UI**.

---

## 5. Regras de negócio na entrada da medição

`OpenScale.addScaleMeasurement()` aplica esta sequência. **A ordem importa.**

1. **Atribuição de usuário** — se `smartUserAssign` estiver ligado,
   `getSmartUserAssignment(peso, 15.0f)` escolhe o perfil cujo último peso está a
   até 15 kg do valor recebido (o mais próximo vence). Se nenhum bater e
   `ignoreOutOfRange` estiver ligado, **a medição é descartada**. Senão, vai para
   o usuário selecionado.
2. **Pesagem assistida** — se o perfil tem `isAssistedWeighing()`, subtrai o peso
   do usuário de referência (para pesar bebê ou pet no colo).
3. **Correção de amputação** — multiplica pelo fator do perfil.
4. **Estimativas**, nesta ordem obrigatória: água → gordura → **LBM por último**,
   porque uma das fórmulas de LBM depende da gordura já calculada. Cada uma só roda
   se a métrica estiver habilitada **e** com estimativa ligada.
5. **Insere** — se já existe medição com a mesma data/hora, o insert falha e o app
   avisa "dado duplicado". É assim que se evita duplicar ao reler o histórico da balança.
6. Dispara widget, alarme e LiveData.

O parâmetro `silent` controla só o toast de confirmação. A UI Bluetooth chama com
`silent=true` (o toast viria em duplicidade com o status).

### `merge()` — atenção ao funcionamento

Fica em `MainActivity` hoje, antes de `addScaleMeasurement`:

```java
if (prefs.getBoolean("mergeWithLastMeasurement", true)) {   // default: LIGADO
    scaleBtData.merge(openScale.getLastScaleMeasurement());
}
```

`ScaleMeasurement.merge()` usa **reflection**: percorre os campos `Float` e copia
do anterior **só onde o atual é `0.0f`**. Serve para pesar na balança e completar
com medidas manuais feitas antes.

Duas implicações:
- Campo que não seja `Float` não é mesclado. Renomear ou trocar o tipo de um campo
  em `ScaleMeasurement` muda esse comportamento **silenciosamente**.
- Um valor legitimamente zero é tratado como "ausente".

Na reconstrução, essa regra deveria migrar de `MainActivity` para `core/` —
é lógica de domínio no lugar errado. Mas **migre preservando o comportamento**.

---

## 6. Pareamento: como a balança é escolhida

`BluetoothSettingsFragment` escaneia e lista dispositivos. Ao escolher, grava
duas preferences:

```java
"btHwAddress"  → MAC          (PREFERENCE_KEY_BLUETOOTH_HW_ADDRESS)
"btDeviceName" → nome anunciado (PREFERENCE_KEY_BLUETOOTH_DEVICE_NAME)
```

**O nome não é cosmético — é o que seleciona o driver.** `createDeviceDriver()`
compara o nome anunciado contra uma lista de prefixos e valores exatos:

```java
if (name.startsWith("beurer bf700")) → BluetoothBeurerSanitas(BEURER_BF700_800_RT_LIBRA)
if (deviceName.startsWith("013197")) → BluetoothMedisanaBS44x(true)   // case-sensitive!
if (deviceName.startsWith("QN-Scale")) → BluetoothQNScale
...
return null;  // não suportado
```

Cuidados:
- Alguns testes usam `name` (minúsculo, via `toLowerCase(Locale.US)`) e outros usam
  `deviceName` (original, **case-sensitive**). Misturar quebra o reconhecimento.
- Se a UI nova reescrever a tela de pareamento, **grave exatamente o nome anunciado**,
  sem trim, sem normalizar caixa, sem "embelezar" para exibição. Guarde o nome cru
  e formate só na hora de mostrar.
- Há um bloco comentado (Beurer BF600/BF850) — suporte desabilitado de propósito.
  Não reative sem hardware para testar.

`BluetoothDebug` é um driver especial que varre todos os serviços GATT e gera log —
é como a comunidade reporta balanças novas. Vale manter acessível em algum canto
dos Ajustes.

---

## 7. Checklist antes de mexer em algo que toca Bluetooth

- [ ] Não editei nada em `core/bluetooth/`.
- [ ] Não inseri valor no meio do enum `BT_STATUS` (só no fim, se precisei).
- [ ] O `Handler` novo trata os **9** estados, não só os felizes.
- [ ] `registerCallbackHandler()` é chamado **antes** de `connect()`.
- [ ] As 5 validações do §4 acontecem antes de conectar.
- [ ] Permissão de localização **e** serviço de localização são verificados.
- [ ] `btDeviceName` é gravado cru, sem normalização.
- [ ] A regra de `merge` foi preservada (default ligado).
- [ ] Chamo `addScaleMeasurement(dados, true)` — com `silent=true`.
- [ ] `disconnectFromBluetoothDevice()` é chamado ao sair da tela de pesagem.
- [ ] Não alterei campos de `ScaleMeasurement` (quebraria o `merge` por reflection).
- [ ] As strings de `SCALE_MESSAGE` continuam existindo em `strings.xml`.

---

## 8. Por que isso exige cuidado extra

- **Não dá para testar aqui.** Exige hardware real; não há mock. O build local
  nem compila (`.claude/docs/build.md`).
- **Falha em silêncio.** Sem exception, sem crash — a balança só não responde.
- **24 protocolos proprietários**, quase todos de engenharia reversa da comunidade,
  cada um com quirks. Não há especificação para consultar.
- **O upstream continua ativo.** Manter `core/bluetooth/` idêntico ao upstream
  facilita puxar suporte a balanças novas.

Se algo parecer errado nessa camada, a resposta certa quase sempre é **relatar,
não consertar** — a menos que haja uma balança em mãos para verificar.
