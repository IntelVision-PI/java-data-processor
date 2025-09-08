# Intelvision - Monitoramento de Servidor

Sistema Java para monitoramento de componentes de servidor (CPU, memória, disco) e status de conexão.

---

## Passo a passo para rodar o projeto

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/javaIntelvision.git
```

### 2. Entre na pasta do projeto

```bash
cd javaIntelvision
```

### 3. Compile e empacote o .jar com todas as dependências

```bash
mvn clean package
```

O Maven irá gerar o arquivo `.jar` dentro da pasta `target/`.

### 4. Confira o .jar gerado

Após o build terminar com sucesso, você deve ter:

```bash
target/intelvision.jar
```

### 5. Rode o programa

```bash
java -jar target/intelvision.jar
```

### 6. Use o sistema

Escolha uma das opções no menu:

- Componentes (CPU, Memória, Disco)
- Status de Conexão com o Banco de Dados
- Status dos Dados do Bucket
- Sair

Digite `s` ou `n` para continuar ou sair do programa.
