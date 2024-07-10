
/*

Dupla:  Gabriel Micael Henrique
        Rodrigo Cabral Diniz
*/
package com.redes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecebeDados extends Thread {

    private final int portaLocalReceber = 2001;
    private final int portaLocalEnviar = 2002;
    private final int portaDestino = 2003;

    private void enviaAck(long numSeq, boolean fim) {
        try {
            //192.168.15.149
            InetAddress address = InetAddress.getByName("localhost");
            try (DatagramSocket datagramSocket = new DatagramSocket(portaLocalEnviar)) {
                String sendString = fim ? "F" : String.valueOf(numSeq);

                byte[] sendData = sendString.getBytes();

                DatagramPacket packet = new DatagramPacket(
                        sendData, sendData.length, address, portaDestino);

                datagramSocket.send(packet);
            } catch (SocketException ex) {
                Logger.getLogger(RecebeDados.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            Logger.getLogger(RecebeDados.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(portaLocalReceber);
            byte[] receiveData = new byte[1404];
            try (FileOutputStream fileOutput = new FileOutputStream("/home/2022.1.08.037/Vídeos/TrabalhoRedes/Recebe/Aula 12 - A linguagem da empresa.pptx")) {
                boolean fim = false;
                long numSeqEsperado = 0;

                while (!fim) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);


                    byte[] tmp = receivePacket.getData();

                    // Extração do número de sequência do pacote
                    ByteBuffer wrapped = ByteBuffer.wrap(receivePacket.getData(), 0, 4);
                    int sequenceNumber = wrapped.getInt();

                    System.out.println( "\033[30;44m" + "Pacote " + sequenceNumber + " recebido" + "\033[m");

                    //Probabilidade de 60% de perda de pacote
//                    Random random = new Random();
//                    float sorteado = random.nextFloat();
//                    if (sorteado < 0.6) {
//                        System.out.println("\033[31m" + "Pacote " + sequenceNumber + " perdido." + "\033[m");
//                        continue; // Não envia ACK e não processa o pacote
//                    }

                    // Verifica se o pacote é o esperado
                    if (sequenceNumber == numSeqEsperado) {
                        int i;
                        // Grava os dados no arquivo de saída
                        for (i = 4; i < receivePacket.getLength(); i = i + 4) {
                            int dados = ((tmp[i] & 0xff) << 24) + ((tmp[i + 1] & 0xff) << 16) + ((tmp[i + 2] & 0xff) << 8) + ((tmp[i + 3] & 0xff));

                            if (dados == -1) {
                                fim = true;
                                break;
                            }
                            fileOutput.write(dados);
                        }

                        if (fim) {
                            System.out.println("Tamanho do pacote: " + i + " bytes.");
                        }else{
                            System.out.println("Tamanho do pacote: " + receivePacket.getLength() + " bytes.");
                        }

                        System.out.println("\033[0;32m" + "Enviando ack de " + numSeqEsperado + "\033[m");
                        enviaAck(numSeqEsperado, fim);
                        numSeqEsperado = (!fim) ? numSeqEsperado + 1 : 0;

                    } else {

                        System.out.println("\033[0;33m" + "Pacote com número de sequência " + sequenceNumber + " não esperado!" + "\033[m");
                        System.out.println("\033[0;32m" + "Enviando ack de " + numSeqEsperado + "\033[m");

                        // Envia ACK do pacote esperado
                        enviaAck(numSeqEsperado, fim);

                    }
                }

            } catch (
                    RuntimeException e) {
                System.out.println(e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("Exceção: " + e.getMessage());
        }
    }
}