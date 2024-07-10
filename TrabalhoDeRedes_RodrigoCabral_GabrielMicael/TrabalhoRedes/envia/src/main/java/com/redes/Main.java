package com.redes;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.exit;

public class Main {
    public static void main(String[] args) {
        Semaphore sem = new Semaphore(5);
        EnviaDados enviaDados = new EnviaDados(sem, "envia");
        EnviaDados ack = new EnviaDados(sem, "ack");
        EnviaDados timeout = new EnviaDados(sem, "timeout");
        ack.start();
        enviaDados.start();
        timeout.start();

        try {
            enviaDados.join();
            ack.join();
            timeout.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("terminou o programa!");
        exit(0);
    }
}