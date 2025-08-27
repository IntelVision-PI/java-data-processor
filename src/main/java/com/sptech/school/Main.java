package com.sptech.school;

import java.util.Random;

public class Main {

    public static void main(String[] args) {
        Random random = new Random();
        int qntd = 10;

        for (int i = 0; i < qntd; i++) {

            int cpu = random.nextInt(101);
            int memoria = random.nextInt(101);
            int disco = random.nextInt(101);

            System.out.println("Monitoramento do Servidor \n");

            Vizualizar("CPU", cpu);
            Vizualizar("Memória", memoria);
            Vizualizar("Disco", disco);

        }
    }
    public static void Vizualizar(String nome, int valor) {
        int total = 10;
        int preenchido = (valor * total) / 100;

        String linha = "";
        for (int i = 0; i < total; i++) {
            if (i < preenchido) {
                linha += "+";
            } else {
                linha += "-";
            }
        }
        System.out.println(nome + ": " + linha + " " + valor + "%");
        if (valor > 90) {
            System.out.println("ALERTA: " + nome + " acima de 90%!");
        }
    }
}
