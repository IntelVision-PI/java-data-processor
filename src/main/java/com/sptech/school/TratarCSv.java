    package com.sptech.school;

    import java.io.*;
    import java.sql.*;
    import java.util.*;

    public class TratarCSv {
        public static void main(String[] args) throws SQLException {
            //nomes arquivos de entrada e saida
            String inputPath = "dados_maquina.csv";
            String outputPath = "csv_tratado.csv";
            //conexão do banco
            Connection conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/intelvision", "intelvision-select", "senha12@");

            //variavel de consulta no banco de dados com parametro posicional
            String consulta="SELECT COUNT(*) FROM servidor WHERE nome = ?";

            //lista dos processos que deve ser ignorado
            List<String> ignorar = Arrays.asList(
                    "System Idle Process",
                    "System",
                    "Idle",
                    "Registry",
                    "smss.exe",
                    "csrss.exe"
            );

            //try iniciando os buffers de leitura e escrita
            try (BufferedReader br = new BufferedReader(new FileReader(inputPath));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {

                //instacio a variavel para mandar para o banco passando a minha variavel de consulta iniciada la em cima
                PreparedStatement pst = conexao.prepareStatement(consulta);

                //variavel de validação para ver se existe ou não o servidor no banco de dados
                boolean existe= false;

                //variavel que ira receber o cabeçalho do csv que esta sendo lido
                String header = br.readLine();
                //verifico se a header tem valor para saber se o meu arquivo csv existe ou não
                if (header == null) {
                    System.out.println("Arquivo CSV vazio!");
                    //caso não existe, ele finaliza com o return
                    return;
                }
                //escrevo no meu csv o meu novo cabeçalho com o top3 ao inves do top5
                bw.write("user;timestamp;cpu;cpu_count;ram;disco;qtd_processos;bytes_recv;package_recv;bytes_sent;package_sent;proc1_name;proc1_cpu_pct;proc2_name;proc2_cpu_pct;proc3_name;proc3_cpu_pct");
                bw.newLine();

                //inicio a variavel que vai receber os dados da linha
                String linha;
                //rodo um while() até que não exista mais dados na variavel "Linha"
                while ((linha = br.readLine()) != null) {
                    //inicio um vetor de String para receber toda a linha lida mas separada com ;
                    String[] colunas = linha.split(";");

                    //salvo nas variaveis os itens salvos no vetor de coluna
                    String user = colunas[0];
                    String timestamp = colunas[1];
                    String cpu = colunas[2];
                    String cpu_count = colunas[3];
                    String ram = colunas[4];
                    String disco = colunas[5];
                    String qtd_processos = colunas[6];
                    String bytes_recv = colunas[7];
                    String package_recv = colunas[8];
                    String bytes_sent = colunas[9];
                    String package_sent = colunas[10];

                    //chamo o pst para executar a consulta no banco de dados passando como parametro para
                    //o meu parametro posicional a variavel user par verificar se o servidor existe no BD
                    pst.setString(1,user);
                    //mando executar a query e salvo oq ele retorna na variavel "rs"
                    ResultSet rs= pst.executeQuery();


                    //faço a verificação do retorno do BD para saber se o servidor existe ou não no banco e passo para a minha variavel existe
                    if (rs.next()) {
                        existe = rs.getInt(1) > 0;
                    }

                    //caso ele n existe, ele volta para o inicio do while
                    if (!existe) {
                        System.out.println("servidor não existe");
                        continue;
                    }else{
                        System.out.println("Servidor existe");
                    }



                    //crio uma lista de Processos da classe "Processo"
                    List<Processo> processos = new ArrayList<>();

                    //faço um for começando do index 11, pq ja passei pelos 10 primeiros salvando nas variaveis em cima
                    //e percorro aq o vetor colunas
                    for (int i = 11; i < colunas.length; i += 2) {
                        //verifico se a dupla (nome e pct) do processo é maior que o vetor, se for, não tem mais processos a percorrer no vetor

                        if (i + 1 < colunas.length) {
                            //pego o valor de nome do processo limpando espaços em branco
                            String nome = colunas[i].trim();
                            // pego o valor a frente do nome i+1 para pegar o cpuPct
                            double cpuPct = Double.parseDouble(colunas[i + 1].trim());

                            //verifico se existe na lista que devo ignorar, caso não exista adiciono a uma lista de processos
                            if (!ignorar.contains(nome)) {
                                processos.add(new Processo(nome, cpuPct));
                            }
                        }
                    }

                    //uso a função sort para ele ordenar os processos pelo cpuPct onde dinamicamente ele vai
                    //validar todos os processos da lista os comparando com a classe Double
                    processos.sort((a, b) -> Double.compare(b.cpuPct, a.cpuPct));

                    //instancio um obj do tipo StringBuilder para construir a linha que ira para o novo csv
                    StringBuilder novaLinha = new StringBuilder();
                    //falo para o StringBuilder juntar tudo(as variaveis que eu passei) com um delimetador de ;
                    novaLinha.append(String.join(";", user, timestamp, cpu, cpu_count, ram, disco,
                            qtd_processos, bytes_recv, package_recv, bytes_sent, package_sent));


                    //faço um for rodar 3 vezes(top3) para pegar os processos da lista
                    for (int i = 0; i < 3; i++) {
                        //verifico o tamanho da lista, caso n tenha o tamanho coloco que não tem nada
                        if (i < processos.size()) {
                            //adiciono ao meu StringBuilder o nome do processo e o cpu
                            novaLinha.append(";").append(processos.get(i).nome)
                                    .append(";").append(processos.get(i).cpuPct);
                        } else {
                            novaLinha.append(";").append("N/A,0");
                        }
                    }


                    //escrevo a linha do StringBuilder no meu csv
                    bw.write(novaLinha.toString());
                    bw.newLine();
                }

                System.out.println("CSV criado em: " + outputPath);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
