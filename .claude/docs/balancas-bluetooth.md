# Balanças Bluetooth — o que nunca quebrar

Documento de segurança. A camada Bluetooth é a parte do app que **falha em
silêncio**: um erro aqui não dá crash nem erro de compilação — a balança
simplesmente para de conectar, ou conecta e não entrega dado, e só se descobre
com hardware real na mão.

**Regra geral: não edite nada em `core/bluetooth/`.** O que a UI precisa é
conversar certo com essa camada. Este documento explica como.

---

## A estrutura

```
core/bluetooth/
├── ScaleFactory.kt          escolhe o handler pelo dispositivo anunciado
├── ScaleCommunicator.kt     a interface + os eventos
├── scales/                  72 handlers, um por protocolo
│   ├── ScaleDeviceHandler   contrato comum
│   ├── GattScaleAdapter     base para balanças que conectam via GATT
│   ├── BroadcastScaleAdapter base para as que só anunciam (sem conexão)
│   └── *Handler.kt          um por fabricante/modelo
└── libs/                    algoritmos de composição corporal por marca
```

São **72 handlers** — contra 24 no fork antigo. A maioria veio de engenharia
reversa da comunidade, sem especificação pública para consultar.

## O contrato com a UI

`ScaleCommunicator` expõe estado como `StateFlow` e eventos como `Flow`:

```kotlin
val isConnecting: StateFlow<Boolean>
val isConnected: StateFlow<Boolean>

fun connect(address: String, scaleUser: ScaleUser?)
fun disconnect()
fun requestMeasurement()
```

Os eventos são uma `sealed class BluetoothEvent`:

| Evento | Quando |
|---|---|
| `Listening` | começou a escutar anúncios (balanças de broadcast) |
| `Connected` / `Disconnected` | conexão GATT |
| `ConnectionFailed` | falhou ao conectar |
| `MeasurementReceived` | **o dado chegou** |
| `BroadcastComplete` | leitura por anúncio terminou |
| `DeviceMessage` | mensagem da própria balança para o usuário |
| `UserInteractionRequired` | a balança pede uma ação (ex.: escolher slot) |
| `Error` | erro inesperado |

**A UI deve tratar todos.** Ignorar `Error` ou `ConnectionFailed` produz
exatamente o sintoma pior: a tela fica esperando para sempre, sem dizer por quê.

Diferente do código antigo, **não há `Handler` com ordinal de enum** — o que
elimina a armadilha de "inserir um valor no meio do enum quebra tudo". Aqui o
`when` sobre a sealed class é exaustivo e o compilador cobra.

## Como a UI fala com isso

Pela `BluetoothFacade`, não diretamente. A facade é a fronteira; os handlers
são detalhe de implementação.

## Dois modos de balança

Isso explica muita coisa no comportamento:

- **GATT** (`GattScaleAdapter`) — conecta, troca dados, desconecta. É o modelo
  clássico.
- **Broadcast** (`BroadcastScaleAdapter`) — a balança **não aceita conexão**;
  ela só transmite o peso em anúncios BLE. O app escuta, lê o pacote e termina.
  Por isso existem `Listening` e `BroadcastComplete` separados de `Connected`.

Uma tela que assume "conectar → receber → desconectar" quebra com as de
broadcast.

## Permissões

Com `minSdk 31`, o modelo é o do Android 12+:

- `BLUETOOTH_SCAN` — para escanear
- `BLUETOOTH_CONNECT` — para conectar **e para ler o nome do dispositivo**

Esse detalhe do nome importa: sem `BLUETOOTH_CONNECT`, `device.name` volta
`null`, o `ScaleFactory` não acha o handler, e a balança aparece como **não
suportada** — sem nenhum erro visível.

Se alguém relatar "minha balança aparece como não suportada", a ordem de
suspeita é:
1. permissão de Bluetooth negada;
2. o modelo realmente não tem handler;
3. o nome anunciado mudou (firmware novo).

## Identificação do dispositivo

`ScaleFactory` decide o handler a partir do que o dispositivo anuncia — nome,
prefixo, às vezes UUID de serviço ou dados de fabricante.

**O nome anunciado é dado, não texto de exibição.** Se uma tela nova de
pareamento for escrita, grave o nome **cru**: sem `trim`, sem normalizar caixa,
sem "embelezar". Formate só na hora de mostrar.

## Checklist antes de mexer em algo que toca Bluetooth

- [ ] Não editei nada em `core/bluetooth/`.
- [ ] Falo com `BluetoothFacade`, não com handler direto.
- [ ] Trato **todos** os `BluetoothEvent`, inclusive `Error` e `ConnectionFailed`.
- [ ] Considerei balanças de broadcast, não só GATT.
- [ ] Peço `BLUETOOTH_SCAN` e `BLUETOOTH_CONNECT` antes de escanear.
- [ ] Desconecto ao sair da tela de pesagem.
- [ ] Se gravo o dispositivo escolhido, gravo o nome cru.

## Por que isso exige cuidado extra

- **Não dá para testar sem hardware.** Não há mock.
- **Falha em silêncio.** Sem exception, sem crash.
- **72 protocolos proprietários**, quase todos de engenharia reversa.
- **O upstream é ativo nesta área.** Manter `core/bluetooth/` idêntico facilita
  puxar suporte a balanças novas — que é justamente o motivo pelo qual este
  fork adotou o 3.1.3.

Se algo parecer errado nessa camada, a resposta certa quase sempre é **relatar
ao upstream, não consertar aqui** — a menos que haja uma balança em mãos para
verificar.
