    package com.sptech.school;

    import java.io.*;
    import java.sql.*;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.util.*;

    public class TratarCSv {
        public static void main(String[] args) throws SQLException {
            //nomes arquivos de entrada e saida
            String inputPath = "dados_maquina.csv";
            String outputPath = "csv_tratado.csv";
            Connection conexao = DriverManager.getConnection(
                    "jdbc:mysql://localhost/intelvision",
                    "intelvision-select",
                    "senha12@"
            );
            Map<String, Map<String, LocalDateTime>> alertasPorServidor = new HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


            String consulta="SELECT COUNT(*) FROM servidor WHERE nome = ?";

            String consultaParametro="select s.nome,\n" +
                    "c.nome as \"Componente\",\n" +
                    "p.alerta_min,\n" +
                    "p.alerta_max\n" +
                    "from servidor s\n" +
                    "inner join parametro p on p.fkServidor = s.id\n" +
                    "right join componente c on c.id = p.fkComponente;";

            //lista dos processos que deve ser ignorado
            List<String> ignorar = Arrays.asList(
                    "System Idle Process",
                    "System",
                    "Idle",
                    "Registry",
                    "smss.exe",
                    "csrss.exe"
            );

            /*{
            "servidor":
                "componente": valores[]
              }
             */
            Map<String, Map<String, double[]>> parametros = new HashMap<>();

            try (BufferedReader br = new BufferedReader(new FileReader(inputPath));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {

                PreparedStatement pst = conexao.prepareStatement(consulta);
                PreparedStatement pst2 = conexao.prepareStatement(consultaParametro);


                boolean existe= false;

                String header = br.readLine();
                if (header == null) {
                    System.out.println("Arquivo CSV vazio!");
                    return;
                }
                bw.write("user;timestamp;cpu;cpu_count;ram;disco;qtd_processos;bytes_recv;package_recv;bytes_sent;package_sent;" +
                        "status_cpu;status_ram;status_disco;" +
                        "proc1_name;proc1_cpu_pct;proc2_name;proc2_cpu_pct;proc3_name;proc3_cpu_pct");
                bw.newLine();;

                String linha;
                while ((linha = br.readLine()) != null) {



                    String[] colunas = linha.split(";");

                    String user = colunas[0].toLowerCase();
                    LocalDateTime timestampData = LocalDateTime.parse(colunas[1], formatter);
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


                    pst.setString(1,user);
                    ResultSet rs= pst.executeQuery();
                    if (rs.next()) {
                        existe = rs.getInt(1) > 0;
                    }

                    if (!existe) {
                        System.out.println("servidor não existe");
                        continue;
                    }else{
                        System.out.println("Servidor existe");
                    }
                    ResultSet rsParametro =pst2.executeQuery();


                    while(rsParametro.next()){
                        String servidor = rsParametro.getString("nome").toLowerCase();
                        String nomeComponente = rsParametro.getString("Componente").toLowerCase();
                        double alerta_min = rsParametro.getDouble("alerta_min");
                        double alerta_max = rsParametro.getDouble("alerta_max");


                        parametros.putIfAbsent(servidor, new HashMap<>());
                        parametros.get(servidor).put(nomeComponente, new double[]{alerta_min, alerta_max});

                    }
                    Map<String, double[]> componentesServidor = parametros.get(user);
                    String statusCpu = "SEM_PARAMETRO";
                    String statusRam = "SEM_PARAMETRO";
                    String statusDisco = "SEM_PARAMETRO";

                    if (componentesServidor != null) {
                        statusCpu = validarComponente("cpu", Double.parseDouble(cpu), componentesServidor, user, alertasPorServidor, timestampData);
                        statusRam = validarComponente("ram", Double.parseDouble(ram), componentesServidor, user, alertasPorServidor, timestampData);
                        statusDisco = validarComponente("disco", Double.parseDouble(disco), componentesServidor, user, alertasPorServidor, timestampData);

                    } else {
                        System.out.println(" Servidor '" + user + "' não tem parâmetros cadastrados!");
                    }

                    List<Processo> processos = new ArrayList<>();

                    for (int i = 11; i < colunas.length; i += 2) {

                        if (i + 1 < colunas.length) {
                            String nome = colunas[i].trim();
                            double cpuPct = Double.parseDouble(colunas[i + 1].trim());

                            if (!ignorar.contains(nome)) {
                                processos.add(new Processo(nome, cpuPct));
                            }
                        }
                    }


                    processos.sort((a, b) -> Double.compare(b.cpuPct, a.cpuPct));

                    StringBuilder novaLinha = new StringBuilder();
                    novaLinha.append(String.join(";", user, (String)timestamp, cpu, cpu_count, ram, disco,
                            qtd_processos, bytes_recv, package_recv, bytes_sent, package_sent,
                            statusCpu, statusRam, statusDisco));


                    for (int i = 0; i < 3; i++) {
                        if (i < processos.size()) {
                            novaLinha.append(";").append(processos.get(i).nome)
                                    .append(";").append(processos.get(i).cpuPct);
                        } else {
                            novaLinha.append(";").append("N/A,0");
                        }
                    }


                    bw.write(novaLinha.toString());
                    bw.newLine();
                }
                if (!alertasPorServidor.isEmpty()) {
                    System.out.println("\n Criando tickets no Jira...\n");

                    for (Map.Entry<String, Map<String, LocalDateTime>> entry : alertasPorServidor.entrySet()) {
                        String servidor = entry.getKey();
                        Map<String, LocalDateTime> alertasMap = entry.getValue();

                        List<String> alertas = new ArrayList<>();
                        for (Map.Entry<String, LocalDateTime> alertaEntry : alertasMap.entrySet()) {
                            String alertaNome = alertaEntry.getKey();
                            LocalDateTime horaAlerta = alertaEntry.getValue();
                            alertas.add(alertaNome + " em " + horaAlerta);
                        }

                        ConexaoJira.criarIssue(servidor, alertas);
                    }
                } else {
                    System.out.println("\n Nenhum alerta crítico detectado. Nenhum ticket criado.");
                }

                System.out.println("CSV criado em: " + outputPath);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static String validarComponente(String nomeComp, double valor, Map<String, double[]> componentes,
                                                String servidor, Map<String, Map<String,LocalDateTime>> alertasPorServidor, LocalDateTime timestamp) {

            if (!componentes.containsKey(nomeComp)) {
                System.out.println(servidor + " não possui o componente '" + nomeComp + "'");
                return "SEM_PARAMETRO";
            }

            double[] limites = componentes.get(nomeComp);
            double min = limites[0];
            double max = limites[1];

            if (valor < min) {
                String alerta = nomeComp.toUpperCase() + " abaixo do mínimo (" + valor + " < " + min + ")";
                System.out.println(servidor + " | " + alerta);

                alertasPorServidor.putIfAbsent(servidor, new HashMap<>());
                alertasPorServidor.get(servidor).put(alerta,timestamp);

                return "ABAIXO";
            } else if (valor > max) {
                String alerta = nomeComp.toUpperCase() + " acima do máximo (" + valor + " > " + max + ")";
                System.out.println(servidor + " | " + alerta);

                alertasPorServidor.putIfAbsent(servidor, new HashMap<>());
                alertasPorServidor.get(servidor).put(alerta,timestamp);

                return "ACIMA";
            } else {
                System.out.println(servidor + " | " + nomeComp + " dentro do limite");
                return "OK";
            }
        }


    }
