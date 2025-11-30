package com.sptech.school;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ConexaoJira {
    static final String jiraUrl= "<URL do jira da sua pagina>";
    static final String usuarioEmail="<seu email do jira>";
    static final String apiToken="<token da sua api do jira>";
    static final String projetoKey="<chave do seu projeto>";

    public static String criarIssue(String servidor, List<String> alertas, String param, String alert) {
        try {


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

    public static void baixarIssues(String caminhoCsv) {
        try {
            String auth = usuarioEmail + ":" + apiToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpClient client = HttpClient.newHttpClient();
            List<Chamado> todos = new ArrayList<>();
            String nextPageToken = null;

            while (true) {
                JSONObject payload = new JSONObject();
                payload.put("jql", "project=CHAMADOS ORDER BY created DESC");
                payload.put("maxResults", 100);
                payload.put("fields", List.of("summary", "labels", "created", "status", "description"));

                if (nextPageToken != null) {
                    payload.put("nextPageToken", nextPageToken);
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(jiraUrl + "/rest/api/3/search/jql"))
                        .header("Authorization", "Basic " + encodedAuth)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    System.err.println("ERRO HTTP: " + response.statusCode());
                    System.out.println(response.body());
                    return;
                }

                JSONObject json = new JSONObject(response.body());
                JSONArray issues = json.optJSONArray("issues");

                if (issues != null && issues.length() > 0) {
                    todos.addAll(parseIssues(response.body()));
                }

                if (!json.has("nextPageToken")) {
                    break;
                }

                nextPageToken = json.getString("nextPageToken");
            }

            System.out.println("TOTAL DE CHAMADOS: " + todos.size());
            exportarParaCsv(caminhoCsv, todos);
            System.out.println("Exportação para CSV concluída: " + caminhoCsv);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Chamado> parseIssues(String json) {
        List<Chamado> lista = new ArrayList<>();

        JSONObject jsonObj = new JSONObject(json);
        JSONArray issues = jsonObj.optJSONArray("issues");
        if (issues == null) return lista;

        for (int i = 0; i < issues.length(); i++) {
            JSONObject issue = issues.optJSONObject(i);
            if (issue == null) continue;

            String id = issue.optString("id", "");
            String key = issue.optString("key", "");

            JSONObject fields = issue.optJSONObject("fields");
            if (fields == null) {
                lista.add(new Chamado(id, key, "", "", "[]", "(sem descrição)"));
                continue;
            }

            String created = fields.optString("created", "");
            JSONArray labelsArray = fields.optJSONArray("labels");
            String labels = labelsArray != null ? labelsArray.toString() : "[]";

            String status = "";
            JSONObject statusObj = fields.optJSONObject("status");
            if (statusObj != null) {
                status = statusObj.optString("name", "");
            }

            String descriptionServ = "(sem descrição)";
            JSONObject descriptionObj = fields.optJSONObject("description");
            if (descriptionObj != null) {
                try {
                    JSONArray content = descriptionObj.optJSONArray("content");
                    if (content != null && content.length() > 0) {
                        JSONObject firstParagraph = content.optJSONObject(0);
                        if (firstParagraph != null) {
                            JSONArray textContent = firstParagraph.optJSONArray("content");
                            if (textContent != null && textContent.length() > 0) {
                                descriptionServ = textContent.optJSONObject(0).optString("text", descriptionServ);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            lista.add(new Chamado(id, key, created, status, labels, descriptionServ));
        }
        return lista;
    }

    public static void exportarParaCsv(String caminho, List<Chamado> chamados) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(caminho))) {

            writer.println("ID;Key;Criado;Status;Labels;Descricao");

            for (Chamado c : chamados) {
                writer.printf("%s;%s;%s;%s;%s;%s%n",
                        transformar(c.getId()),
                        transformar(c.getKey()),
                        transformar(c.getCreated()),
                        transformar(c.getStatus()),
                        transformar(c.getLabels()),
                        transformar(c.getDescription())
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String transformar(String campo) {
        if (campo == null) return "";

        if (campo.startsWith("[") && campo.endsWith("]")) {
            return campo;
        }

        campo = campo.replace("\"", "\"\"");
        return "\"" + campo + "\"";
    }
}
