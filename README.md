# Checkpoint 5 — Bug Hunt PetFiap

## Identificação


| Integrant | 2CCPW
*Gabriel Simioni - RM563475*
*Guilherme Vega - RM562655*
*Davi Xavier - RM563572*
*Gabriel Pereira -RM563571*
*Luiz henrique- RM563795*
*Felipe Ramalho - RM565073*

| Campo | |
|---|---|
| **Total de bugs corrigidos** | 12 / 12 |
| **Total de ajustes de Clean Code** | 6 / 6 |
| **Total de testes novos escritos** | 6 / 6 |
| **Suíte final (Run As → JUnit Test)** | 26 testes (20 entregues + 6 novos), 0 falhas |

---

## Parte 1 — Bugs encontrados

| # | Sintoma observado (o que fiz/vi) | Causa raiz (arquivo e linha aproximada) | Correção aplicada | Conceito da disciplina |
|---|---|---|---|---|
| bug01 | `GeradorProtocoloTest`: `expected: <GeradorProtocolo@5432050b> but was: <...@75f2099>` e protocolos `expected: <2> but was: <1>` (dois testes vermelhos, mesma causa) | `GeradorProtocolo.getInstancia()` (~l.15): retornava `new GeradorProtocolo()` e nunca atribuía o campo `instancia`; cada chamada criava um gerador com contador zerado. Além disso `proximo()` não era thread-safe, apesar do comentário da classe | Atribui `instancia = new GeradorProtocolo()` e marca `getInstancia()` e `proximo()` como `synchronized` | Padrão Singleton (Aula 14), estado estático, concorrência |
| bug02 | `AtendimentoFactoryTest`: `Unexpected type, expected: <Tosa> but was: <Banho>` | `AtendimentoFactory.criar` (~l.15): `case "TOSA" -> new Banho(...)` (copiar/colar) | `case "TOSA" -> new Tosa(...)` | Padrão Factory (Aula 14), polimorfismo |
| bug03 | `AtendimentoFactoryTest`: `expected: <Mimi> but was: <null>` ao criar CONSULTA | `ConsultaVeterinaria(int, String, ...)` (~l.17): chamava `super()` sem argumentos, descartando protocolo, pet, porte, tutor e data (e o status ficava `null`) | `super(protocolo, petNome, petPorte, tutorNome, dataHora)` | Construtores e herança (`super`), encapsulamento |
| bug04 | `AtendimentoBuilderTest`: `expected: <Rex> but was: <null>` | `AtendimentoBuilder.comPet` (~l.25): `petNome = petNome;` atribuía o parâmetro a ele mesmo (faltava `this.`) e o campo ficava `null` | `this.petNome = petNome;` | `this`, sombreamento de variáveis, Builder (Aula 14) |
| bug05 | `AtendimentoBuilderTest`: `Expected IllegalArgumentException to be thrown, but nothing was thrown` (sem nome e sem porte) | `AtendimentoBuilder.construir` (~l.40): não validava nada ("fica por conta do controller"), então o objeto nascia inválido | `construir()` lança `IllegalArgumentException` se `petNome` ou `petPorte` forem nulos/em branco | Builder que só entrega objeto válido, exceções (Aula 11) |
| bug06 | `AgendaServiceTest`: agendamento duplicado não lançava `HorarioOcupadoException` (apareceu como `NullPointerException` no `save()` do mock, pois o conflito passou batido) | `AgendaService.agendar` (~l.24): `a.getPetNome() == novo.getPetNome()` e `a.getDataHora() == novo.getDataHora()` comparavam referências, não valores | `.equals()` nas duas comparações | `==` vs `.equals()` (Aula 7), identidade vs igualdade |
| bug07 | `AgendaServiceTest`: `Expected AtendimentoNaoEncontradoException to be thrown, but nothing was thrown` | `AgendaService.buscarPorId` (~l.37): `try/catch (Exception e) { return null; }` engolia a exceção do `orElseThrow`; o controller nunca devolvia 404 e `concluir`/`cancelar` davam `NullPointerException` | Removido o `try/catch`: a exceção chega a quem chamou | Exceções customizadas unchecked (Aula 11), catch genérico é anti-padrão |
| bug08 | `BanhoTest` novo (teste01) vermelho: `expected: <60.0> but was: <100.0>` | `Banho.calcularPreco` (~l.28): valores invertidos (PEQUENO 100, GRANDE 60) | PEQUENO 60, MEDIO 80, GRANDE 100 | Polimorfismo / regra de negócio no model (Aula 14) |
| bug09 | `TosaTest` novo (teste02) vermelho: `expected: <60> but was: <30>` | `Tosa` (~l.35): declarava `getDuracaoMinutos(String porte)` (sobrecarga) em vez de sobrescrever `getDuracaoMinutos()`; valia a implementação da classe pai (30) | Método passou a ser `@Override public int getDuracaoMinutos()` | Sobrescrita vs sobrecarga, `@Override` (Aula 7) |
| bug10 | `AtendimentoStatusTest` novo (teste04) vermelho: `Expected StatusInvalidoException to be thrown, but nothing was thrown` | `Atendimento.cancelar` (~l.58): atribuía `CANCELADO` sem checar o status atual (cancelava atendimento já realizado ou já cancelado) | `cancelar()` só aceita `AGENDADO`; senão lança `StatusInvalidoException` (mesma regra do `concluir()`) | Encapsulamento de regras no model, máquina de estados, exceções (Aula 11) |
| bug11 | `AgendaServiceTest` novo (teste06) vermelho: `expected: IllegalArgumentException but was: NullPointerException` (agendou no passado e ainda consultou o banco) | `AgendaService.agendar` (~l.20): nenhuma validação de data/hora | Primeira linha de `agendar()`: se `dataHora.isBefore(now)` lança `IllegalArgumentException`, antes de qualquer acesso ao repository | Validação precoce (fail fast), exceções, testes com Mockito (Aula 15) |
| bug12 | Nenhum teste unitário acusa (só lendo o código / subindo a API: o `POST /api/atendimentos` falharia no `save()`) | `Atendimento.id` (~l.17): `@Id` sem `@GeneratedValue`; o Hibernate exige id atribuído manualmente e a API não atribui | `@GeneratedValue(strategy = GenerationType.IDENTITY)` | JPA/Hibernate (Aulas 12/13), geração de chave primária |

## Parte 2 — Ajustes de Clean Code

| # | Onde estava | Qual princípio/boas práticas era violado | O que eu mudei |
|---|---|---|---|
| clean01 | `AtendimentoFactory.criar(int p, String t, String n, String po, String tu, LocalDateTime d)` | Nomes que revelam a intenção: parâmetros de uma letra/abreviados | Renomeados para `protocolo, tipo, petNome, petPorte, tutorNome, dataHora` |
| clean02 | `System.out.println` no construtor de `GeradorProtocolo` e o "Recibo" em `AgendaService.agendar` | Sem efeitos colaterais/debug em código de produção; service não deve imprimir no console | Removidos os dois `println`; `agendar` retorna direto `repository.save(novo)` |
| clean03 | `@Autowired` em campo privado no `AgendaService` e no `AtendimentoController` | Injeção por construtor: dependências explícitas, campos `final`, testável sem reflexão | Construtores recebendo `AtendimentoRepository` / `AgendaService`, campos `final`, `@Autowired` removido |
| clean04 | Strings mágicas `"AGENDADO"`, `"CONCLUIDO"`, `"CANCELADO"` espalhadas em `Atendimento` e `AgendaService` | Sem números/strings mágicos: um erro de digitação vira bug silencioso | Constantes `STATUS_AGENDADO/CONCLUIDO/CANCELADO` em `Atendimento`, usadas nos dois arquivos |
| clean05 | `calcularDescontoFidelidade` (private, nunca chamado, com comentário "futuro") no controller | Sem código morto/comentado; além disso a regra do comentário (10% acima de 500 pontos) nem batia com o cálculo | Método e bloco de comentário removidos |
| clean06 | `ResponseEntity.status(201)` e `status(409)` no controller | Sem números mágicos: código autoexplicativo | Trocados por `HttpStatus.CREATED` e `HttpStatus.CONFLICT` |

## Parte 3 — Testes novos (regras que estavam sem cobertura)

| # | Teste escrito (classe.método) | Regra coberta | Resultado ao escrever (vermelho/verde) |
|---|---|---|---|
| teste01 | `BanhoTest.deveCobrarPrecoDoBanhoConformeOPorte` | Banho: PEQUENO R$ 60, MEDIO R$ 80, GRANDE R$ 100 | **Vermelho** — revelou bug08 (preços invertidos) |
| teste02 | `TosaTest.deveDurar60Minutos` | Tosa dura 60 minutos | **Vermelho** — revelou bug09 (sobrecarga em vez de sobrescrita) |
| teste03 | `ConsultaVeterinariaTest.deveCustar150ReaisIndependenteDoPorte` | Consulta: R$ 150 fixo, porte não muda o preço | **Verde** de cara — regra já estava correta |
| teste04 | `AtendimentoStatusTest.deveRecusarCancelamentoQuandoAtendimentoJaConcluido` | `cancelar()` em CONCLUIDO recusa (`StatusInvalidoException`) | **Vermelho** — revelou bug10 |
| teste05 | `AtendimentoStatusTest.deveCancelarQuandoAtendimentoEstaAgendado` | `cancelar()` em AGENDADO vira CANCELADO | **Verde** de cara — regra já estava correta |
| teste06 | `AgendaServiceTest.deveRecusarAgendamentoQuandoDataHoraJaPassou` | Data/hora no passado: `IllegalArgumentException` e o banco nem é consultado | **Vermelho** — revelou bug11 |

> Os 20 testes entregues não foram alterados; os testes novos foram apenas acrescentados
> (nas classes existentes) ou criados em classe nova (`AtendimentoStatusTest`).

---

## Parte 4 — Perguntas de reflexão

### 1. A suíte como contrato (Aula 15)
Rodei a suíte e li cada mensagem de falha como um sintoma. `expected: <Rex> but was: <null>` no `AtendimentoBuilderTest` apontou direto para o Builder: o campo nunca foi preenchido, e a causa era `petNome = petNome;` sem `this`. `expected: <2> but was: <1>` junto com dois objetos diferentes no teste do singleton revelou que `getInstancia()` criava um gerador novo toda vez. Os testes também mostraram bugs em cascata: o conflito de horário aparecia como `NullPointerException` porque a verificação com `==` deixava passar e o `save()` do mock devolvia `null`. Diferente do curl, a suíte roda em segundos, sem banco, repete sempre igual e protege contra regressão: depois de cada correção rodei tudo e vi que nada quebrou, coisa que testar à mão não garante.

### 2. Mock e injeção de dependência (Aulas 13 a 15)
Em produção, o Spring cria o `AtendimentoRepository` (uma implementação gerada pelo Spring Data, ligada ao Oracle) e injeta no `AgendaService`; hoje isso é feito pelo construtor, antes era o `@Autowired`. No `AgendaServiceTest`, quem faz o papel do Spring é o Mockito: `@Mock` cria um repository falso e `@InjectMocks` o entrega ao service. Como o service depende só da interface `AtendimentoRepository`, ele não sabe se do outro lado há Oracle ou um mock. Por isso o teste roda sem banco e sem subir o Spring: com `when(repository.findByPetNome("Rex")).thenReturn(List.of())` eu controlo exatamente o que o "banco" responde. O mock é literal: só faz o que foi ensinado, e `verify(repository, never()).save(any())` prova que nada foi salvo no conflito.

### 3. `==` vs `.equals()` (Aula 7)
O `AgendaService` comparava `a.getPetNome() == novo.getPetNome()` e `a.getDataHora() == novo.getDataHora()`. `==` compara referências (se é o mesmo objeto na memória), não conteúdo. Com literais como `"Rex"` costuma "funcionar por sorte" porque o Java reaproveita a mesma String no pool de literais; mas um nome vindo de `@RequestParam` ou do banco é um objeto novo, e `LocalDateTime` nunca é reaproveitado. No `deveRecusarAgendamentoComHorarioJaOcupado` o teste cria o mesmo horário em outro objeto de propósito (`LocalDateTime.parse(...)`), reproduzindo o mundo real, e o `==` falhava. Troquei por `.equals()`, que compara o valor, e o conflito passou a ser detectado.

### 4. Sobrescrita vs sobrecarga (Aula 7)
A `Tosa` declarava `getDuracaoMinutos(String porte)`. Isso tem o mesmo nome, mas assinatura diferente: é uma sobrecarga (um método novo, ao lado do herdado), não uma sobrescrita. Compila sem erro, mas quando alguém chama `getDuracaoMinutos()` sem argumento (como o controller, via polimorfismo, e o teste), o Java executa o método da classe pai, que devolve 30 em vez de 60. Com `@Override` o compilador exigiria que o método existisse na superclasse com a mesma assinatura e acusaria erro na hora; a `Banho` já usava `@Override` e por isso estava certa. A correção foi trocar a assinatura para `@Override public int getDuracaoMinutos()`.

### 5. Singleton manual vs bean do Spring (Aula 14)
O `GeradorProtocolo` garante uma única instância (construtor privado + campo estático) e, com isso, uma numeração global sequencial. O bug: `getInstancia()` fazia `return new GeradorProtocolo()` e nunca guardava o resultado em `instancia`, então cada chamada criava um gerador novo com contador zerado, e todos os protocolos saíam 1. Corrigi guardando a instância e, de quebra, sincronizei `getInstancia()` e `proximo()` (a classe se dizia thread-safe e não era). O `AgendaService` não corre esse risco porque quem controla o ciclo de vida é o container do Spring: um `@Service` é um bean singleton por padrão, criado e guardado uma vez pelo container; ninguém escreve `new AgendaService()` nem o `if (instancia == null)` à mão.

### 6. Cobertura de testes: onde parar? (Aula 15)
Vale manter os verdes. O teste03 (consulta com preço fixo) e o teste05 (cancelar agendado) passaram de cara, mas são regras do contrato que um refactor futuro pode quebrar, como aconteceu com o `cancelar()`, que era permissivo demais. Eles custam milissegundos e viram rede de segurança. Com prazo apertado, eu priorizaria primeiro o caminho feliz das regras de negócio centrais (agendar, preço, status) e logo depois os caminhos de erro, porque foram eles que esconderam bugs aqui: passado, conflito de horário e cancelar concluído. Os 4 bugs ocultos estavam em regras de recusa. Perseguir 100% de cobertura não é a meta: getters/setters e código sem regra pouco acrescentam; melhor cobrir regras com risco de negócio.

---

## Parte 5 — Espaço livre (opcional)

Notas de revisão (fora do escopo da correção, não alteradas):

- `AtendimentoController.agendar` consome um número do `GeradorProtocolo` antes de validar a requisição; um pedido inválido (400) "queima" um protocolo.
- O contador do `GeradorProtocolo` fica só em memória e reinicia em 0 quando a aplicação reinicia; protocolos podem repetir com os já gravados no banco.
