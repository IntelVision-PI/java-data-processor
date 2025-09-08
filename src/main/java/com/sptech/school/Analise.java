package com.sptech.school;
import java.time.format.DateTimeFormatter;
import java.util.Random; // para rodar os números de maneira aleatória

public class Analise {
    private String nome;
    private DateTimeFormatter dataDaVizualizacao;
    private Boolean ativa;


    public static void monitorarComponentes(Random random) {
        int cpu = random.nextInt(101);
        int memoria = random.nextInt(101);
        int disco = random.nextInt(101);

        System.out.println("\nMonitoramento do Servidor:");
        visualizar("CPU", cpu);
        visualizar("Memória", memoria);
        visualizar("Disco", disco);
    }

    public static void visualizar(String nome, int valor) {
        int total = 10;
        int preenchido = (valor * total) / 100;

        StringBuilder linha = new StringBuilder();
        for (int i = 0; i < total; i++) {
            linha.append(i < preenchido ? "●" : "○");
        }
        System.out.println(nome + ": " + linha + " " + valor + "%");
        if (valor > 90) {
            System.out.println("ALERTA: " + nome + " acima de 90%!");
        } else if (valor < 10) {
            System.out.println("AVISO: " + nome + " abaixo de 10%!");
        }
    }
}