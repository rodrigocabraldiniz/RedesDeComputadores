
/*

Dupla:  Gabriel Micael Henrique
        Rodrigo Cabral Diniz
*/

package com.redes;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.sql.Time;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EnviaDados extends Thread {

    private final int portaLocalEnvio = 2000;
    private final int portaDestino = 2001;
    private final int portaLocalRecebimento = 2003;
    private static volatile Semaphore sem;
    private static volatile int[] dados;
    private static volatile int contDoUltimoPacote;
    private final String funcao;
    private static volatile int numSeq;
    private static int ultimoSeqRecebida = -1;
    private static volatile boolean timeout = false;
    private static volatile boolean terminou = false;
    private static volatile HashMap<Integer, Timer> times = new HashMap<Integer, Timer>();
    private static volatile boolean retrasmissao = false;


    public EnviaDados(Semaphore sem, String funcao) {
        super(funcao);
        this.sem = sem;
        this.funcao = funcao;
    }

//    public Timer timeout() {
//        Timer timer = new Timer();
//
//        TimerTask task = new TimerTask() {
//            @Override
//            public void run() {
//                if(!timeout){
//                    timeout = true;
//                    cancelAllTimers();
//                    System.out.println("Timeout ocorreu no pacote. Reenviando pacotes perdidos...");
//                    retrasmissao = true;
//                    reenviaPacotesPerdidos();
//
//                }
//            }
//        };
//        // Inicia o timer para 10ms (tempo ajustável conforme necessário)
//        timer.schedule(task, 100);
//        return timer;
//    }
//
//    public void cancelAllTimers() {
//        synchronized (times) {
//            for (Timer timer : times.values()) {
//                timer.cancel();
//            }
//            times.clear();
//        }
//    }

    private void enviaPct(int[] dados) {
        // Converte int[] para byte[]
        ByteBuffer byteBuffer = ByteBuffer.allocate(dados.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(dados);

        byte[] buffer = byteBuffer.array();


            try {
                System.out.println("Semaforo: " + sem.availablePermits());
                sem.acquire();
                System.out.println("Semaforo: " + sem.availablePermits());

                //192.168.15.145 endereço
                InetAddress address = InetAddress.getByName("localhost");
                try (DatagramSocket datagramSocket = new DatagramSocket(portaLocalEnvio)) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, portaDestino);

                    datagramSocket.send(packet);
                }
                System.out.println("\033[0;34m" + "Pacote " + dados[0] + " enviado." + "\033[0m");


            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
            }

    }

    private void reenviaPacotesPerdidos() {
        try (FileInputStream fileInput = new FileInputStream("/home/2022.1.08.037/Vídeos/TrabalhoRedes/envia/Aula 12 - A linguagem da empresa.pptx")) {



            int pontoDePartida = ultimoSeqRecebida;
            long pulandoArquivoJaLido = pontoDePartida * 1400L;

            System.out.println("Pulando " + fileInput.skip(pulandoArquivoJaLido) + " Bytes.");
            int lido;
            dados = new int[351];
            int cont = 1;
            numSeq = pontoDePartida;

            System.out.println("Reenviando pacote a partir do número de sequência: " + numSeq);

            sem = new Semaphore(5);

            while ((lido = fileInput.read()) != -1) {
                dados[cont] = lido;
                cont++;
                contDoUltimoPacote = cont;
                if (cont == 351) {
                    dados[0] = numSeq;
//                    if(timeout){
//                        timeout = false;
//                        reenviaPacotesPerdidos();
//                        return;
//                    }
//                    times.put(numSeq, timeout());
                    enviaPct(dados);
//                    System.out.println("Iniciando timer para pacote " + numSeq);
                    numSeq++;
                    cont = 1;
                }
            }


            for (int i = contDoUltimoPacote; i < 351; i++) {
                dados[i] = -1;
            }
            dados[0] = numSeq;
            enviaPct(dados);

        } catch (IOException e) {
            System.out.println("Error message: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        switch (this.funcao) {
            case "envia":
                dados = new int[351];
                int cont = 1;
                numSeq = 0;
                try (FileInputStream fileInput = new FileInputStream("/home/2022.1.08.037/Vídeos/TrabalhoRedes/envia/Aula 12 - A linguagem da empresa.pptx")) {

                    System.out.println("Tamanho do arquivo: " + fileInput.available() + " Bytes");

                    int lido;
                    while ((lido = fileInput.read()) != -1) {
                        dados[cont] = lido;
                        cont++;
                        contDoUltimoPacote = cont;
                        if (cont == 351) {
                            dados[0] = numSeq;
//                            times.put(numSeq, timeout());
                            enviaPct(dados);
//                            System.out.println("Iniciando timer para pacote " + numSeq);
                            numSeq++;
                            cont = 1;
                        }
                    }

                    if(!retrasmissao) {
                        for (int i = contDoUltimoPacote; i < 351; i++) {
                            dados[i] = -1;
                        }
                        dados[0] = numSeq;
                        enviaPct(dados);
                    }

                } catch (IOException e) {
                    System.out.println("Error message: " + e.getMessage());
                }
                break;
            case "ack":
                try {
                    int acktmp = -1, ackatual, receivedSeq = -1;
                    DatagramSocket serverSocket = new DatagramSocket(portaLocalRecebimento);
                    String retorno = "";
                    while (!retorno.equals("F")) {
                        byte[] receiveData = new byte[12];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);
                        retorno = new String(receivePacket.getData()).trim();
                        retorno = retorno.replace("null", "").trim();
                        if (!retorno.equals("F")) {
                            try {
                                receivedSeq = Integer.parseInt(retorno);
                                if (ultimoSeqRecebida + 1 == receivedSeq) {
                                    ultimoSeqRecebida = receivedSeq;
                                    sem.release();
//                                    if(times.get(receivedSeq) != null){
//                                        times.get(receivedSeq).cancel();
//                                    }
//                                    times.remove(receivedSeq);
                                }
                                System.out.println("\033[0;32m" + "Ack recebido " + receivedSeq + "." + "\033[m");

                            } catch (NumberFormatException e) {
                                System.err.println("Erro ao converter string para número inteiro: " + e.getMessage());
                            }

                        } else {
                            terminou = true;
                            System.out.println("\033[0;32m" + "Ack recebido " + (receivedSeq + 1) + "." + "\033[m");
                        }

                    }
                    ultimoSeqRecebida = 0;
                } catch (IOException e) {
                    System.out.println("Excecao: " + e.getMessage());
                }
                break;
            case "timeout":
                break;
            default:
                break;
        }
    }
}
