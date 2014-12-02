
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.text.SimpleDateFormat;
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
 * @author Elton Sia A008008541 & Thilina Ratnayake A00802338
 */
public class HostA {

    //The byteContainer to hold the controlCommand  and send Datas
    public static byte[] receiveCommandContainer = new byte[8];
    public static byte[] sendData = new byte[90];
    //The max amount of packets we're going to send
    public static int pax;
    //The size of our window
    public static int window;
    //Our sequenceNumber pool
    public static int seqNum;
    //Boolean that determines which mode Host will be in
    public static boolean sendMode;
    //IP address to connect to network
    public static InetAddress networkAddr;
    //Port to connect to network
    public static int networkPort;
    //Port to listen from network
    public static int listenPort;
    //Timer object that determines if a packet times out
    public static Timer timer;
    //Container that acts as our WINDOW
    public static ArrayList<Packet> packetsContainer;
    //The amount of time that we determine that a packet can remain UN ACKED
    public static long timeOutLength;
    //All packets that are acked go into this container for final comparison
    public static ArrayList<Packet> ackedPacketsContainer;
    //Writer that writes to console
    public static PrintWriter writer;
    //timeStamp to go into every written message
    public static String timeStamp;
    //the port which we will get commands from the network for CONTROL MESSAGES only.
    public static int controlPort;

    public static ArrayList<Packet> remainingPacketsContainer;

    public static void main(String args[]) throws Exception {

        Config config = new Config();

        //HOST DEPENDANT CHANGES
        controlPort = Integer.parseInt(config.getProp().getProperty("aControlPort"));  
        networkPort = Integer.parseInt(config.getProp().getProperty("hostAnetworkPort"));
        listenPort = Integer.parseInt(config.getProp().getProperty("hostAlistenPort"));
        writer = new PrintWriter("HostAlog.txt", "UTF-8");
       
        //END CHANGES
        
        //Set data from reading CONFIG file
        pax = Integer.parseInt(config.getProp().getProperty("pacs"));
        window = Integer.parseInt(config.getProp().getProperty("windowsize")); 
        seqNum = Integer.parseInt(config.getProp().getProperty("sequenceNum"));
        timeOutLength = Integer.parseInt(config.getProp().getProperty("delay")) * 4;
        
        String networkAddress = config.getProp().getProperty("hostAToNet");
        networkAddr = InetAddress.getByName(networkAddress);
        System.out.println("IP for network is " + networkAddr);
        writer.println(timeStamp() + ": " + "IP for network is " + networkAddr);

        //Create a socket that listens to CONTROL MESSAGES from NETWORK.
        DatagramSocket commandSocket = new DatagramSocket(controlPort);
        int startCommand = listenForCommand(commandSocket);
        System.out.println("Start Command is " + startCommand);
        writer.println(timeStamp() + ": " + "Start Command is " + startCommand);

        //Create new packetsContainer to hold packets (this is essentially our window)
        packetsContainer = new ArrayList<>();
        ackedPacketsContainer = new ArrayList<>();

        
        //Depending on the start commands,, go into SEND or RECEIVE mode.
        if (startCommand == 1) {
            sendMode = true;
            System.out.println("Go into SENDER mode");
            writer.println(timeStamp() + ": " + "Go into SENDER mode");
        } else {
            sendMode = false;
            System.out.println("Go into RECEIVER mode");
            writer.println(timeStamp() + ": " + "Go into RECEIVER mode");
            RECEIVEACK();
        }

        while (true) {
            //While in SENDMODE do SEND, when you're done SENDING, set SendMode to false and BREAK to go into RECEIVEACK mode
            while (sendMode) {
                SEND();
            }
            RECEIVE();
        }

    }

    /** This method listens on a specified socket for CONTROL messages from server
     * 
     * @param commandSocket is the Socket that will be used to LISTEN on for commands
     * @return 
     */    
    public static int listenForCommand(DatagramSocket commandSocket) {
        int command = 0;
        while (true) {
            try {
                System.out.println("Host A waiting for a command from NETWORK....");
                writer.println(timeStamp() + ": " + "Host A waiting for a command from NETWORK....");
                DatagramPacket receiveCommandPacket = new DatagramPacket(receiveCommandContainer, receiveCommandContainer.length);
                commandSocket.receive(receiveCommandPacket);
                String receivedCommand = new String(receiveCommandPacket.getData());
                receivedCommand = receivedCommand.trim();
                System.out.println("RECEIVED NESSAGE FROM NETWORK: " + receivedCommand);
                writer.println(timeStamp() + ": " + "RECEIVED NESSAGE FROM NETWORK: " + receivedCommand);
                command = Integer.valueOf(receivedCommand);
                break;
            } catch (IOException ex) {
                Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return command;
    }
    
    /** The method that SENDS packets to the Network.
     *  This method does a number of things:
     *  1: (Dependant on factors) Creates the packetContainer (WINDOW) and populates it with X packets.
     *  2: Sends packets to the network.
     * 
     * 
     */
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

                    writer.println(timeStamp() + ": " + "Packet #" + packet.getSeqNum() + " added to container");
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

                    writer.println(timeStamp() + ": " + "Packet #" + packet.getSeqNum() + " added to container");
                    //Increase the sequence number
                    seqNum++;
                }

            } else if (seqNum > pax) {
                System.out.println("The seqNum is greater than MAX so don't send anymore!");
                writer.println(timeStamp() + ": " + "The seqNum is greater than MAX so don't send anymore!");

            }

            //Send those packets
            sendPackets(packetsContainer);
        //Send those pax

            //go into receiveMode
            sendMode = false;
        } else {
            //System.out.println("seqNum " + seqNum);
            if (seqNum > pax) {
                writer.println(timeStamp() + ": " + "seqNum > MAX, CEASE sending.");
                writer.println(timeStamp() + ": " + "# of pax in container: " + packetsContainer.size());

                //If there's still pax in the container, keep resending till they get through
                if (packetsContainer.size() > 0) {
                    sendPackets(packetsContainer);
                }
            } else {
                System.out.println("seqNum @ max, LASTPACKET SCENARIO");

                //EMPTY window scenario
                if (packetsContainer.isEmpty()) {
                    try {
                        Packet packet = new Packet(3, seqNum, 5, seqNum);
                        DatagramSocket sendSocket = new DatagramSocket();
                        HostA.sendPacket(packet, sendSocket);
                        sendSocket.close();
                        System.out.println("EOT packet SENT!");
                        writer.println(timeStamp() + ": " + "**EOT PACKET SENT!**");
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
                    System.out.println(timeStamp() + ": " + "Packet #" + packet.getSeqNum() + " added to container");

                    HostA.sendPackets(packetsContainer);

                }
            }
        }
    }

    /**Breaks down (serializes) a packet OBJECT into a DatagramPacket to be ready to send by 
     * socket
     * 
     * 
     * @param packet Take in a created packet object
     * @return 
     */
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

    /** Method grabs an arrayList of packets, preps them and sends them to Network
     * 
     * @param packetContainer 
     */
    public static void sendPackets(ArrayList<Packet> packetContainer) {
        try {
            DatagramSocket sendSocket = new DatagramSocket();
            for (Packet packetObj : packetContainer) {

                //Check if EOT
                if (packetObj.getSeqNum() == pax) {
                    packetObj.setPacketType(3);
                    writer.println(timeStamp() + ": " + "SENT | #" + packetObj.getSeqNum() + " | " + paxType(packetObj.getPacketType()) + " TO NETWORK");
                    writer.println("--EOT HAS BEEN SENT BACK, CLOSING LOG --");
                    writer.close();
                }
                sendData = prepPacket(packetObj);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, networkAddr, networkPort);
                sendSocket.send(sendPacket);
                System.out.println("SENT | #" + packetObj.getSeqNum() + " | " + paxType(packetObj.getPacketType()) + " TO NETWORK");
                writer.println(timeStamp() + ": " + "SENT | #" + packetObj.getSeqNum() + " | " + paxType(packetObj.getPacketType()) + " TO NETWORK");
            }

            timer = new Timer();
            //HOST DEPENDANT
            timer.schedule(new timeOut("A") {
            }, timeOutLength);
            sendSocket.close();
            System.out.println("Last pax sent, timer created, socket closed \n\n");
            writer.println(timeStamp() + ": " + "Last packet in current window SENT. TIMER STARTED\n\n");
        } catch (SocketException ex) {
            Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

     /** Takes a single packet object, Serializes it, and sends it off.
      * 
      * @param packetObj
      * @param sendSocket 
      */
    public static void sendPacket(Packet packetObj, DatagramSocket sendSocket) {

        try {
            //check if EOT
            if (packetObj.getPacketType() == 3) {
                //set type to EOT
                packetObj.setPacketType(5);
                writer.println(timeStamp() + ": " + "SENT | #" + packetObj.getSeqNum() + " | " + paxType(packetObj.getPacketType()) + " TO NETWORK");
                writer.println("--EOT HAS BEEN SENT BACK, CLOSING LOG --");
                writer.close();
            }
            sendData = prepPacket(packetObj);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, networkAddr, networkPort);
            sendSocket.send(sendPacket);
            System.out.println("SENT | #" + packetObj.getSeqNum() + " | " + paxType(packetObj.getPacketType()) + " TO NETWORK");
            writer.println(timeStamp() + ": " + "SENT | #" + packetObj.getSeqNum() + " | " + paxType(packetObj.getPacketType()) + " TO NETWORK");
        } catch (SocketException ex) {
            Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /** This method RECEIVES packets to facilitate two way communication.
     * Once a packet is received, print it out.
     * Then remove it from WINDOW.
     * Check the array to see if its empty (UNACKED)
     * If the array is empty, that means all packets have been ACKED (none in flight)
     * So keep going.
     *
     */
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

                    System.out.println("RCVD | #" + packet.getSeqNum() + " | " + paxType(packet.getPacketType()));
                    writer.println("RCVD | #" + packet.getSeqNum() + " | " + paxType(packet.getPacketType()));

                    //check if EOT END STATE
                    removeInWindow(packet.getSeqNum());

                    if (packet.getPacketType() == 5 && packetsContainer.isEmpty()) {
                        System.out.println("\n **********END OF SESSION***************\n");
                        writer.println(timeStamp() + ": " + "\n *********END OF SESSION COMPLETE******** \n");
                        writer.close();
                        System.exit(seqNum);
                    }
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

                    System.out.println("RCVD | #" + packet.getSeqNum() + " | " + paxType(packet.getPacketType()));
                    writer.println("RCVD | #" + packet.getSeqNum() + " | " + paxType(packet.getPacketType()));

                    //If the packet is not an EOT, 
                    if (packet.getPacketType() == 1) {

                        //Convert the packet into an ACK
                        packet.setPacketType(2);
                    } else if (packet.getPacketType() == 3) {
                        System.out.println("GOT AN EOT, SETTING TO EOTACK");
                        packet.setPacketType(5);

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

    public static void removeInWindow(int sequenceNum) {

        //remove from window 
        for (int i = 0; i < packetsContainer.size(); i++) {
            if (packetsContainer.get(i).getSeqNum() == sequenceNum) {
                ackedPacketsContainer.add(packetsContainer.get(i));
                packetsContainer.remove(i);

            }
        }

        System.out.println("Remove in window method , size at end = " + packetsContainer.size());

    }

    /** Check if array is empty. 
     * If array is empty, it indicates that all packets have been acked.
     * Therefore, go into summary.
     * 
     */
    public static void checkArray() {
        //if array empty means all packets acked. STOP TIMER
        if (packetsContainer.isEmpty()) {
            timer.cancel();
            timer.purge();
            System.out.println("Array empty. All Pax arrived. Timer Stop");
            writer.println(timeStamp() + ": " + "Array empty. All Pax arrived. Timer Stop");
            postConversation();
        }
    }

    /** Once a packet times out. Cancel timer, proceed to summary of transmission.
     * 
     */
    public static void TIMEOUT() {
        //Cancel the timer
        timer.cancel();
        timer.purge();

        //go to postConversation
        postConversation();
    }

    /** Summarizes triggering packet exchange.
     * explains packets lost, packets succesfully delivered.
     * 
     */
    public static void postConversation() {
        try {
            System.out.println("\n POST TRANSMISSION STATS");
            writer.println(timeStamp() + ": " + "POST TRANSMISSION STATS \n");
            System.out.println("LAST TRANSMISSION WINDOW SIZE:" + window);
            writer.println(timeStamp() + ": " + "LAST TRANSMISSION WINDOW SIZE:" + window);
            System.out.println(HostA.packetsContainer.size() + "PACKETS LOST");
            writer.println(timeStamp() + ": " + HostA.packetsContainer.size() + "PACKETS LOST");
            int success = window - packetsContainer.size();
            System.out.println(success + "/" + window + "packets received successfully");
            writer.println(timeStamp() + ": " + success + "/" + window + "packets received successfully");
            System.out.println("Pausing for 5 seconds before sending again");
            writer.println(timeStamp() + ": " + "Pausing for 5 seconds before sending again");
            Thread.sleep(5000);
            writer.println(timeStamp() + ": " + "Sleep Finished! sending again");
            sendMode = true;
            HostA.SEND();

        } catch (InterruptedException ex) {
            Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(HostA.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** Translates packet types to human readable short form. For easier reading 
     * on the output.
     * 
     * @param packetType packet type number which corresponds to a specific type.
     * @return 
     */
    public static String paxType(int packetType) {
        String type = null;
        switch (packetType) {
            case 1:
                type = "PSH";
                break;
            case 2:
                type = "ACK";
                break;
            case 3:
                type = "EOT";
                break;
            case 4:
                type = "LOSS";
                break;
            case 5:
                type = "EOTACK";
                break;
        }

        return type;
    }

    /**
     * Purely just for displaying time stamps in prints.
     * @return 
     */
    public static String timeStamp() {
        return timeStamp = new SimpleDateFormat("yyyy,MM,dd_HH:mm:ss").format(Calendar.getInstance().getTime());
    }

}
