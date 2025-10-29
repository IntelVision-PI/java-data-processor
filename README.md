#  TratarCSV

## Visão Geral

O **TratarCSV** é um programa Java desenvolvido para tratar e enriquecer dados de monitoramento de servidores.  
Ele lê um arquivo CSV com métricas, cruza essas informações com dados do banco MySQL e gera um novo arquivo padronizado contendo o status operacional de cada componente e os processos mais relevantes.

---

## Funcionalidades

- Leitura e tratamento de CSVs com dados de servidores.  
- Consulta ao banco de dados para validação de existência e parâmetros.  
- Verificação dos limites de **CPU**, **RAM** e **Disco** com base nos alertas cadastrados.  
- Filtragem de processos irrelevantes e listagem dos 3 com maior consumo de CPU.  
- Geração de um novo CSV consolidado com status e métricas tratadas.

---

## Estrutura do Projeto

src/ 

└── com/sptech/school/ 

├── TratarCSv.java 

└── Processo.java 

---

## Fluxo de Execução

1. Lê o arquivo `dados_maquina.csv`.  
2. Verifica se o servidor está cadastrado no banco.  
3. Obtém os parâmetros de alerta (mínimo e máximo).  
4. Classifica o status de cada componente como:
   - **OK**
   - **ACIMA**
   - **ABAIXO**
   - **SEM_PARAMETRO**
5. Filtra e ordena os processos por consumo de CPU.  
6. Gera o arquivo `csv_tratado.csv` com as informações consolidadas.

---

## Banco de Dados

O programa realiza consultas para:
- Validar a existência de servidores.
- Buscar parâmetros de alerta dos componentes monitorados.

Tabelas esperadas:
- **servidor**
- **parametro**
- **componente**

---

## Configuração da Conexão

Edite a linha de conexão no código substituindo pelos valores do seu ambiente:

jdbc:mysql://<IP EC2>/<BANCO>



---

## Saída Esperada

O arquivo `csv_tratado.csv` incluirá:
- Dados originais do servidor.  
- Status dos componentes.  
- Três processos com maior uso de CPU.

Exemplo de cabeçalho:
user;timestamp;cpu;ram;disco;...;status_cpu;status_ram;status_disco;proc1_name;proc1_cpu_pct;...

---

## Requisitos

- Java SE 8 ou superior  
- Driver JDBC do MySQL  
- Banco de dados MySQL 8.0+

---

## Execução

```bash
javac -d bin src/com/sptech/school/*.java
java -cp bin com.sptech.school.TratarCSv
O arquivo csv_tratado.csv será gerado no diretório do projeto.

Autor
Nicolas Barbosa
Projeto desenvolvido para automação e tratamento de dados de servidores monitorados na plataforma IntelVision.











O ChatGPT pode 
