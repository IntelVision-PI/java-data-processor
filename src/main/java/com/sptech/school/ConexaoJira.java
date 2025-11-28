package com.sptech.school;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;

public class ConexaoJira {

    public static String criarIssue(String servidor, List<String> alertas, String param, String alert) {
        try {





            String jiraUrl= "<URL do jira da sua pagina>";
            String usuarioEmail="<seu email do jira>";
            String apiToken="<token da sua api do jira>";
            String projetoKey="<chave do seu projeto>";



            String auth = usuarioEmail + ":" + apiToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());


            StringBuilder descricaoJson = new StringBuilder();
            //o jira tem exigencias para criar um arquivo, ele usa o ADF(Atlassian Document Format)
            //o type:doc significa que estamos usando esse docuemento
            //o version é a versao do rich text(o jira só aceita essa versão e qujalquer outra retorna error400)
            //contente é um array de objeto de paragrafo
            descricaoJson.append("{\"type\":\"doc\",\"version\":1,\"content\":[");
            //ja dentro do array content,
            descricaoJson.append("{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"" + alertas.size() + " alertas críticos foram detectados no servidor " + servidor + ":\"}]},");

            for (String alerta : alertas) {
                descricaoJson.append("{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"" + alerta.replace("\"", "\\\"") + "\"}]}," );
            }

            descricaoJson.append("{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Por favor, verifique o servidor imediatamente.\"}]}");

            descricaoJson.append("]}");

            String json = "{\n" +
                    "  \"fields\": {\n" +
                    "    \"project\": {\"key\": \"" + projetoKey + "\"},\n" +
                    "    \"summary\": \"Alertas críticos detectados - Servidor " + servidor + "\",\n" +
                    "    \"description\": " + descricaoJson.toString() + ",\n" +
                    "    \"issuetype\": {\"name\": \"Task\"}\n" +
                    "    \"labels\": [\"" + param + "\", \"" + alert + "\"]\n" +
                    "  }\n" +
                    "}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jiraUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + encodedAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println(" Ticket criado para servidor " + servidor + " | Status: " + response.statusCode());
            System.out.println("JSON enviado ao Jira:");
            System.out.println(json);

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao criar issue: " + e.getMessage();
        }
    }
}
