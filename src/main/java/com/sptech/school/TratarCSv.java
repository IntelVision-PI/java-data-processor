package com.sptech.school;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TratarCSv implements RequestHandler<S3Event, String> {

    @Override
    public String handleRequest(S3Event event, Context context) {

        AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();


        if (event.getRecords() == null || event.getRecords().isEmpty()) {
            return "Nenhum registro S3 recebido!";
        }


        String bucketRaw = event.getRecords().get(0).getS3().getBucket().getName();
        String keyRaw = event.getRecords().get(0).getS3().getObject().getKey();

        File inputFile = new File("/tmp/input.csv");
        try {
            s3.getObject(bucketRaw, keyRaw).getObjectContent().transferTo(new FileOutputStream(inputFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        File outputFile = new File("/tmp/output.csv");
        try {
            processarCsv(inputFile.getPath(), outputFile.getPath());
        } catch (Exception e) {
            return "Erro no processamento: " + e.getMessage();
        }

        String bucketTrusted = "my-bucket-trusted";

        String[] partes = keyRaw.split("/");

        if (partes.length != 4) {
            throw new RuntimeException("Key do S3 inesperada, deve ter 4 partes: " + keyRaw);
        }

        String ano = partes[0];
        String mes = partes[1];
        String dia = partes[2];
        String fileName = partes[3];

        String keyTrusted = String.format("%s/%s/%s/%s", ano, mes, dia, fileName);

        s3.putObject(bucketTrusted, keyTrusted, outputFile);

        return "Processado e salvo em: " + keyTrusted;
    }



    public void processarCsv(String inputPath, String outputPath) throws SQLException {

        Connection conexao = DriverManager.getConnection(
                "jdbc:mysql://52.23.99.63:3306/intelvision",
                "intelvision-select",
                "senha12@"
        );

        Map<String, Map<String, LocalDateTime>> alertasPorServidor = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String consulta="SELECT COUNT(*) FROM servidor WHERE nome = ?";
        String consultaParametro="select s.nome, c.nome as \"Componente\", p.alerta,"
                + " p.em_risco_min, p.em_risco_max "
                + "from servidor s inner join parametro p on p.fkServidor = s.id "
                + "right join componente c on c.id = p.fkComponente;";

        List<String> ignorar = Arrays.asList("System Idle Process", "System", "Idle", "Registry", "smss.exe", "csrss.exe");

        Map<String, Map<String, double[]>> parametros = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(inputPath));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {

            PreparedStatement pst = conexao.prepareStatement(consulta);
            PreparedStatement pst2 = conexao.prepareStatement(consultaParametro);

            boolean existe;
            String header = br.readLine();
            if (header == null) return;

            bw.write("user;timestamp;cpu;cpu_count;ram;disco;disco_size_gb;qtd_processos;bytes_recv;package_recv;bytes_sent;package_sent;"
                    + "status_cpu;status_ram;status_disco;"
                    + "proc1_name;proc1_cpu_pct;proc2_name;proc2_cpu_pct;proc3_name;proc3_cpu_pct");
            bw.newLine();

            String linha;
            while ((linha = br.readLine()) != null) {

                String[] colunas = linha.split(";");

                String user = colunas[0].toLowerCase();
                LocalDateTime timestampData = LocalDateTime.parse(colunas[1], formatter);

                pst.setString(1, user);
                ResultSet rs = pst.executeQuery();
                existe = rs.next() && rs.getInt(1) > 0;

                if (!existe) continue;

                ResultSet rsParametro = pst2.executeQuery();

                while (rsParametro.next()) {
                    String servidor = rsParametro.getString("nome").toLowerCase();
                    String nomeComponente = rsParametro.getString("Componente").toLowerCase();
                    double em_risco_min = rsParametro.getDouble("em_risco_min");
                    double em_risco_max = rsParametro.getDouble("em_risco_max");
                    double alerta = rsParametro.getDouble("alerta");

                    parametros.putIfAbsent(servidor, new HashMap<>());
                    parametros.get(servidor).put(nomeComponente, new double[]{alerta, em_risco_min, em_risco_max});
                }

                Map<String, double[]> componentesServidor = parametros.get(user);
                if (componentesServidor == null) continue;

                // status
                String statusCpu = validarComponente("cpu", Double.parseDouble(colunas[2]), componentesServidor, user, alertasPorServidor, timestampData);
                String statusRam = validarComponente("ram", Double.parseDouble(colunas[4]), componentesServidor, user, alertasPorServidor, timestampData);
                String statusDisco = validarComponente("disco", Double.parseDouble(colunas[5]), componentesServidor, user, alertasPorServidor, timestampData);

                List<Processo> processos = new ArrayList<>();

                for (int i = 12; i < colunas.length; i += 2) {
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

                novaLinha.append(String.join(";",
                        colunas[0],
                        colunas[1],
                        colunas[2],
                        colunas[3],
                        colunas[4],
                        colunas[5],
                        colunas[6],
                        colunas[7],
                        colunas[8],
                        colunas[9],
                        colunas[10],
                        colunas[11],
                        statusCpu,
                        statusRam,
                        statusDisco
                ));



                for (int i = 0; i < 3; i++) {
                    if (i < processos.size()) {
                        novaLinha.append(";").append(processos.get(i).nome)
                                .append(";").append(processos.get(i).cpuPct);
                    } else {
                        novaLinha.append(";N/A;0");
                    }
                }

                bw.write(novaLinha.toString());
                bw.newLine();
            }
            if (!alertasPorServidor.isEmpty()) {
                System.out.println("Criando tickets no Jira...");
                for (Map.Entry<String, Map<String, LocalDateTime>> entry : alertasPorServidor.entrySet()) {
                    String servidor = entry.getKey();
                    Map<String, LocalDateTime> alertasMap = entry.getValue();

                    List<String> alertas = new ArrayList<>();
                    for (Map.Entry<String, LocalDateTime> alertaEntry : alertasMap.entrySet()) {
                        String alertaNome = alertaEntry.getKey();
                        LocalDateTime horaAlerta = alertaEntry.getValue();
                        alertas.add(alertaNome + " em " + horaAlerta);
                    }

                    String resultado = ConexaoJira.criarIssue(servidor, alertas);
                    System.out.println("Resultado Jira: " + resultado);
                }
            } else {
                System.out.println("Nenhum alerta crítico detectado. Nenhum ticket criado.");
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
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
