import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author User
 */
public class HostA {

    //The byteContainer to hold the controlCommand 
    public static byte[] receiveCommandContainer = new byte[8];

    public static byte[] sendData = new byte[90];

    //The max amount of packets we're going to send
    public static int pax;

    //The size of our window
    public static int window;

    //Our sequenceNumber pool
    public static int seqNum;

    public static boolean sendMode;

    public static InetAddress networkAddr;

    public static int networkPort;

    public static int listenPort;

    public static Timer timer;

    public static ArrayList<Packet> packetsContainer;

    public static long timeOutLength;

    public static ArrayList<Packet> ackedPacketsContainer;

    public static void main(String args[]) throws Exception {

        pax = 15;
        window = 5;
        //START SEQUENCE NUMBERS FROM 1!
        seqNum = 1;

        networkAddr = InetAddress.getByName("localhost");
        networkPort = 7005;

        listenPort = 8006;

        timeOutLength = 500;
        
        System.out.println("Please enter in the IP address for Network");
        Scanner scan = new Scanner(System.in);
        String networkAddress = scan.nextLine();
        networkAddr = InetAddress.getByName(networkAddress);
        System.out.println("IP for network is "+networkAddr);

        //New socket purely meant for listening to commands from NET
        DatagramSocket commandSocket = new DatagramSocket(5004);
        int startCommand = listenForCommand(commandSocket);
        System.out.println("Start Command is " + startCommand);

        //Create new packetsContainer to hold packets (this is essentially our window)
        packetsContainer = new ArrayList<>();

        ackedPacketsContainer = new ArrayList<>();

        if (startCommand == 1) {
            sendMode = true;
            System.out.println("Go into SENDER mode");
        } else {
            sendMode = false;
            System.out.println("Go into RECEIVER mode");
            RECEIVEACK();
        }

        //SEND();
        //Infinite loop
        while (true) {

            //While in SENDMODE do SEND, when you're done SENDING, set to false and BREAK to go into RECEIVEACK mode
            while (sendMode) {
                SEND();
            }
            RECEIVE();
        }

    }

    public static int listenForCommand(DatagramSocket commandSocket) {
        int command = 0;
        while (true) {
            try {
                System.out.println("Host A waiting for a command from NETWORK....");
                DatagramPacket receiveCommandPacket = new DatagramPacket(receiveCommandContainer, receiveCommandContainer.length);
                commandSocket.receive(receiveCommandPacket);
                String receivedCommand = new String(receiveCommandPacket.getData());
                receivedCommand = receivedCommand.trim();
                System.out.println("RECEIVED NESSAGE FROM NETWORK: " + receivedCommand);
                command = Integer.valueOf(receivedCommand);
                break;
            } catch (IOException ex) {
                Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return command;
    }

    public static void SEND() {

        //Only do this  if sequence number is != max
        if (seqNum < pax) {
            //1. EMPTY WINDOW Scenario: Starting from seqNum 1 -> window, add a packet into packet Container
            if (packetsContainer.size() == 0) {
                for (int i = 1; i <= window; i++) {
                    //Create the packets to send
                    Packet packet = new Packet(1, seqNum, 5, seqNum);
                    //Add those packets to the container (window)
                    packetsContainer.add(packet);
                    //System.out.println("Packet created for " + i);
                    System.out.println("Packet #" + packet.getSeqNum() + " added to container");
                    //Increase the sequence number
                    seqNum++;
                }
            } //2. SOMEWHAT FULL WINDOW Scenario 2: Some pax were dropped, which means window still has some pax. e.g. 1,2,3. START @ 4
            else if (seqNum == pax) {

                for (int i = packetsContainer.size() + 1; i <= window; i++) {
                    //Create the packets to send
                    Packet packet = new Packet(1, seqNum, 5, seqNum);
                    //Add those packets to the container (window)
                    packetsContainer.add(packet);
                    //System.out.println("Packet created for " + i);
                    System.out.println("Packet #" + packet.getSeqNum() + " added to container");
                    //Increase the sequence number
                    seqNum++;
                }

            } else if (seqNum > pax) {
                System.out.println("The seqNum is greater than MAX so don't send anymore!");
            }

            //Send those packets
            sendPackets(packetsContainer);
        //Send those pax

            //go into receiveMode
            sendMode = false;
        } else {
            System.out.println("seqNum " + seqNum);
            if (seqNum > pax) {
                System.out.println("The seqNum is greater than MAX so don't send anymore!");
                System.out.println("Num of pax still in container " + packetsContainer.size());

                //If there's still pax in the container, keep resending till they get through
                if (packetsContainer.size() > 0) {
                    sendPackets(packetsContainer);
                } else {
                    System.out.println("Packets container is size " + packetsContainer.size());
                    System.out.println("That means all PAX HAVE BEEN ACKED! BOOM !");
                    System.out.println("Check this, PAX #" + pax);
                    System.out.println("All ackec pax size = " + ackedPacketsContainer.size());
                }
            } else {
                System.out.println("seqNum @ max, LASTPACKET SCENARIO");

                //EMPTY window scenario
                if (packetsContainer.size() == 0) {
                    try {
                        Packet packet = new Packet(3, seqNum, 5, seqNum);
                        DatagramSocket sendSocket = new DatagramSocket();
                        HostA.sendPacket(packet, sendSocket);
                        sendSocket.close();
                        System.out.println("EOT packet SENT!");
                    } catch (SocketException ex) {
                        Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } //SOMEWHATFUL window scenario
                else {
                    //Create the packets to send
                    Packet packet = new Packet(3, seqNum, 5, seqNum);
                    //Add those packets to the container (window)
                    packetsContainer.add(packet);
                    //System.out.println("Packet created for " + i);
                    System.out.println("Packet #" + packet.getSeqNum() + " added to container");

                    HostA.sendPackets(packetsContainer);

                }
            }
        }
    }

    public static byte[] prepPacket(Packet packet) {
        //Serialize packet object into a bytearray
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream os;
        try {
            os = new ObjectOutputStream(outputStream);
            os.writeObject(packet);
        } catch (IOException ex) {
            Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
        }

        return outputStream.toByteArray();
    }

    public static void sendPackets(ArrayList<Packet> packetContainer) {
        try {
            DatagramSocket sendSocket = new DatagramSocket();
            for (Packet packetObj : packetContainer) {

                //Check if EOT
                if (packetObj.getSeqNum() == pax) {
                    packetObj.setPacketType(3);
                }
                sendData = prepPacket(packetObj);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, networkAddr, networkPort);
                sendSocket.send(sendPacket);
                System.out.println("Packet #" + packetObj.getSeqNum() + " SENT!");
            }

            timer = new Timer();
            timer.schedule(new timeOut("A") {
            }, timeOutLength);
            sendSocket.close();
            System.out.println("Last pax sent, timer created, socket closed");
        } catch (SocketException ex) {
            Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void sendPacket(Packet packetObj, DatagramSocket sendSocket) {

        try {
            //check if EOT
            if (packetObj.getSeqNum() == pax) {
                //set type to EOT
                packetObj.setPacketType(3);
            }
            sendData = prepPacket(packetObj);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, networkAddr, networkPort);
            sendSocket.send(sendPacket);
            System.out.println("Packet #" + packetObj.getSeqNum() + " SENT!");
        } catch (SocketException ex) {
            Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void RECEIVE() {

        try {
            System.out.println("Inside receive()");
            byte[] receiveData = new byte[90];
            System.out.println("socket created, waiting to receive");
            DatagramSocket listenSocket = new DatagramSocket(listenPort);
            while (true) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    listenSocket.receive(receivePacket);
                    String received = new String(receivePacket.getData());

                    receiveData = receivePacket.getData();

                    ByteArrayInputStream in = new ByteArrayInputStream(receiveData);
                    ObjectInputStream is = new ObjectInputStream(in);

                    Packet packet = (Packet) is.readObject();

                    System.out.println("RECEIVED PACKET " + packet.getSeqNum());

                    removeInWindow(packet.getSeqNum());

                    checkArray();

                } catch (IOException ex) {
                    Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (SocketException ex) {
            Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void RECEIVEACK() {
        System.out.println("IN RECEIVE MODE");
        try {
            System.out.println("Inside receiveAck");
            byte[] receiveData = new byte[90];
            System.out.println("socket created, waiting to receive");
            DatagramSocket listenSocket = new DatagramSocket(listenPort);
            while (true) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    listenSocket.receive(receivePacket);
                    String received = new String(receivePacket.getData());

                    receiveData = receivePacket.getData();

                    ByteArrayInputStream in = new ByteArrayInputStream(receiveData);
                    ObjectInputStream is = new ObjectInputStream(in);

                    Packet packet = (Packet) is.readObject();

                    System.out.println("RECEIVED PACKET " + packet.getSeqNum());

                    //If the packet is not an EOT, 
                    if (packet.getPacketType() != 3) {

                        //Convert the packet into an ACK
                        packet.setPacketType(2);
                    }

                    DatagramSocket sendSocket = new DatagramSocket();
                    sendPacket(packet, sendSocket);
                    sendSocket.close();

                } catch (IOException ex) {
                    Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (SocketException ex) {
            Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void setLoss() {

    }

    public static void removeInWindow(int sequenceNum) {

        //remove from window 
        for (int i = 0; i < packetsContainer.size(); i++) {
            if (packetsContainer.get(i).getSeqNum() == sequenceNum) {
                ackedPacketsContainer.add(packetsContainer.get(i));
                packetsContainer.remove(i);

            }
        }

    }

    public static void checkArray() {
        //if array empty means all packets acked. STOP TIMER
        if (packetsContainer.size() == 0) {
            timer.cancel();
            timer.purge();
            System.out.println("Array empty. All Pax arrived. Timer Stop");
            postConversation();
        }
    }

    public static void TIMEOUT() {
        //Cancel the timer
        timer.cancel();
        timer.purge();

        //go to postConversation
        postConversation();
    }

    public static void postConversation() {
        try {
            System.out.println("For that last transmission, window size was " + window);
            System.out.println(HostA.packetsContainer.size() + "PAX LOST");
            int success = window - packetsContainer.size();
            float successRate = success / window;
            System.out.println("Success rate is " + (successRate * 100) + "%");
            System.out.println("Sleeping for 5 seconds");
            Thread.sleep(5000);
            System.out.println("Sleep Finished! sending again");
            sendMode = true;
            HostA.SEND();

        } catch (InterruptedException ex) {
            Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}


