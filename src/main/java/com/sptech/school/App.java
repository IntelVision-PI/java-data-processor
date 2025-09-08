package com.sptech.school;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Scanner;

import static com.sptech.school.Analise.monitorarComponentes;

public class App {
    public static void main(String[] args) {
        Random random = new Random();
        Scanner scanner = new Scanner(System.in);

        String companyName = "Intelbras";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        System.out.println("Empresa: " + companyName);
        System.out.println("Visualizando em: " + dtf.format(now));

        System.out.println(
                "\n" +
                        "░█▀▀░█▀█░█▀█░▀█▀░█░█░█▀▄░█▀█░░░█▀▄░█▀▀░░░█▀▄░█▀█░█▀▄░█▀█░█▀▀\n" +
                        "░█░░░█▀█░█▀▀░░█░░█░█░█▀▄░█▀█░░░█░█░█▀▀░░░█░█░█▀█░█░█░█░█░▀▀█\n" +
                        "░▀▀▀░▀░▀░▀░░░░▀░░▀▀▀░▀░▀░▀░▀░░░▀▀░░▀▀▀░░░▀▀░░▀░▀░▀▀░░▀▀▀░▀▀▀\n"
        );

        boolean continuar = true;

        while (continuar) {
            System.out.println("\nEscolha o que deseja visualizar:");
            System.out.println("1. Componentes (CPU, Memória, Disco)");
            System.out.println("2. Status de Conexão com o Banco de Dados");
            System.out.println("3. Status dos Dados do Bucket");
            System.out.println("4. Sair");

            int leitor = scanner.nextInt();

            switch (leitor) {
                case 1 -> monitorarComponentes(random);
                case 2 -> System.out.println("Status de Conexão com o Banco de Dados: CONECTADO");
                case 3 -> System.out.println("Status dos Dados do Bucket: NÃO ESTÁ RECEBENDO DADOS");
                case 4 -> continuar = false;
                default -> System.out.println("Opção inválida!");
            }

            if (continuar) {
                System.out.println("\nDeseja continuar? (s/n)");
                String resposta = scanner.next();
                if (resposta.equalsIgnoreCase("n")) {
                    continuar = false;
                }
            }
        }
        System.out.println("Saindo do programa. Obrigado!");
        scanner.close();
    }
}
