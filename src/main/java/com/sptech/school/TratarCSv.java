    package com.sptech.school;

    import java.io.*;
    import java.sql.*;
    import java.util.*;

    public class TratarCSv {
        public static void main(String[] args) throws SQLException {
            String inputPath = "dados_maquina.csv";
            String outputPath = "csv_tratado.csv";
            Connection conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/intelvision", "intelvision-select", "senha12@");

            String consulta="SELECT COUNT(*) FROM servidor WHERE nome = ?";


            List<String> ignorar = Arrays.asList(
                    "System Idle Process",
                    "System",
                    "Idle",
                    "Registry",
                    "smss.exe",
                    "csrss.exe"
            );

            try (BufferedReader br = new BufferedReader(new FileReader(inputPath));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {


                PreparedStatement pst = conexao.prepareStatement(consulta);


                boolean existe= false;




                String header = br.readLine();
                if (header == null) {
                    System.out.println("Arquivo CSV vazio!");
                    return;
                }

                bw.write("user,timestamp,cpu,cpu_count,ram,disco,qtd_processos,bytes_recv,package_recv,bytes_sent,package_sent,proc1_name,proc1_cpu_pct,proc2_name,proc2_cpu_pct,proc3_name,proc3_cpu_pct");
                bw.newLine();

                String linha;
                while ((linha = br.readLine()) != null) {
                    String[] colunas = linha.split(";");

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
                    novaLinha.append(String.join(",", user, timestamp, cpu, cpu_count, ram, disco,
                            qtd_processos, bytes_recv, package_recv, bytes_sent, package_sent));

                    for (int i = 0; i < 3; i++) {
                        if (i < processos.size()) {
                            novaLinha.append(",").append(processos.get(i).nome)
                                    .append(",").append(processos.get(i).cpuPct);
                        } else {
                            novaLinha.append(",").append("N/A,0");
                        }
                    }

                    bw.write(novaLinha.toString());
                    bw.newLine();
                }

                System.out.println("CSV criado em: " + outputPath);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
