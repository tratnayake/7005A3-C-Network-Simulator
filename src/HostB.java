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
 * @author User
 */
public class HostB {

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
    
    public static PrintWriter writer;
    
    public static String timeStamp;

    public static int controlPort;
    
    public static void main(String args[]) throws Exception {
        
        Config config = new Config();
        
        
        controlPort = Integer.parseInt(config.getProp().getProperty("bControlPort"));
        
        //CHANGE THESE FOR EVERY NEW HOST
        networkPort = Integer.parseInt(config.getProp().getProperty("hostBnetworkPort"));

        listenPort = Integer.parseInt(config.getProp().getProperty("hostBlistenPort"));
        
         writer = new PrintWriter("HostBlog.txt","UTF-8");
         
         
         System.out.println("FInished writing");
        //END CHANGES
        
        networkAddr = InetAddress.getByName("localhost");
        
        pax = Integer.parseInt(config.getProp().getProperty("pacs"));
        window = Integer.parseInt(config.getProp().getProperty("windowsize"));
        //START SEQUENCE NUMBERS FROM 1!
        seqNum = Integer.parseInt(config.getProp().getProperty("sequenceNum"));

        

        timeOutLength = Integer.parseInt(config.getProp().getProperty("delay")) * 3;
        
        String networkAddress = config.getProp().getProperty("hostBToNet");
        networkAddr = InetAddress.getByName(networkAddress);
        System.out.println("IP for network is "+networkAddr);
        writer.println(timeStamp()+": "+"IP for network is "+networkAddr);

        //New socket purely meant for listening to commands from NET
        DatagramSocket commandSocket = new DatagramSocket(controlPort);
        int startCommand = listenForCommand(commandSocket);
        System.out.println("Start Command is " + startCommand);
        writer.println(timeStamp()+": "+"Start Command is " + startCommand);

        //Create new packetsContainer to hold packets (this is essentially our window)
        packetsContainer = new ArrayList<>();

        ackedPacketsContainer = new ArrayList<>();

        if (startCommand == 1) {
            sendMode = true;
            System.out.println("Go into SENDER mode");
            writer.println(timeStamp()+": "+"Go into SENDER mode");
        } else {
            sendMode = false;
            System.out.println("Go into RECEIVER mode");
            writer.println(timeStamp()+": "+"Go into RECEIVER mode");
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
                writer.println(timeStamp()+": "+"Host A waiting for a command from NETWORK....");
                DatagramPacket receiveCommandPacket = new DatagramPacket(receiveCommandContainer, receiveCommandContainer.length);
                commandSocket.receive(receiveCommandPacket);
                String receivedCommand = new String(receiveCommandPacket.getData());
                receivedCommand = receivedCommand.trim();
                System.out.println("RECEIVED NESSAGE FROM NETWORK: " + receivedCommand);
                writer.println(timeStamp()+": "+"RECEIVED NESSAGE FROM NETWORK: " + receivedCommand);
                command = Integer.valueOf(receivedCommand);
                break;
            } catch (IOException ex) {
                Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
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
                    
                    writer.println(timeStamp()+": "+"Packet #" + packet.getSeqNum() + " added to container");
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
                    
                    writer.println(timeStamp()+": "+"Packet #" + packet.getSeqNum() + " added to container");
                    //Increase the sequence number
                    seqNum++;
                }

            } else if (seqNum > pax) {
                System.out.println("The seqNum is greater than MAX so don't send anymore!");
                writer.println(timeStamp()+": "+"The seqNum is greater than MAX so don't send anymore!");
                
            }

            //Send those packets
            sendPackets(packetsContainer);
        //Send those pax

            //go into receiveMode
            sendMode = false;
        } else {
            //System.out.println("seqNum " + seqNum);
            if (seqNum > pax) {
                writer.println(timeStamp()+": "+"seqNum > MAX, CEASE sending.");
                writer.println(timeStamp()+": "+"# of pax in container: " + packetsContainer.size());

                //If there's still pax in the container, keep resending till they get through
                if (packetsContainer.size() > 0) {
                    sendPackets(packetsContainer);
                } else {
                    System.out.println("\n **********END OF SESSION***************\n");
                    //System.out.println("Packets container is size " + packetsContainer.size());
                    //System.out.println("That means all PAX HAVE BEEN ACKED! BOOM !");
                    //System.out.println("Check this, PAX #" + pax);
                    //System.out.println("All ackec pax size = " + ackedPacketsContainer.size());
                    writer.println(timeStamp()+": "+"\n *********END OF SESSION COMPLETE******** \n");
                    writer.close();
                }
            } else {
                System.out.println("seqNum @ max, LASTPACKET SCENARIO");

                //EMPTY window scenario
                if (packetsContainer.size() == 0) {
                    try {
                        Packet packet = new Packet(3, seqNum, 5, seqNum);
                        DatagramSocket sendSocket = new DatagramSocket();
                        HostB.sendPacket(packet, sendSocket);
                        sendSocket.close();
                        System.out.println("EOT packet SENT!");
                        writer.println(timeStamp()+": "+"**EOT PACKET SENT!**");
                    } catch (SocketException ex) {
                        Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } //SOMEWHATFUL window scenario
                else {
                    //Create the packets to send
                    Packet packet = new Packet(3, seqNum, 5, seqNum);
                    //Add those packets to the container (window)
                    packetsContainer.add(packet);
                    //System.out.println("Packet created for " + i);
                    System.out.println(timeStamp()+": "+"Packet #" + packet.getSeqNum() + " added to container");

                    HostB.sendPackets(packetsContainer);

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
            Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
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
                    writer.println(timeStamp()+": "+"SENT | #"+packetObj.getSeqNum()+" | "+paxType(packetObj.getPacketType())+" TO NETWORK");
                writer.println("--EOT HAS BEEN SENT BACK, CLOSING LOG --");
                    writer.close();
                }
                sendData = prepPacket(packetObj);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, networkAddr, networkPort);
                sendSocket.send(sendPacket);
                System.out.println("SENT | #"+packetObj.getSeqNum()+" | "+paxType(packetObj.getPacketType())+" TO NETWORK");
                writer.println(timeStamp()+": "+"SENT | #"+packetObj.getSeqNum()+" | "+paxType(packetObj.getPacketType())+" TO NETWORK");
            }

            timer = new Timer();
            timer.schedule(new timeOut("B") {
            }, timeOutLength);
            sendSocket.close();
            System.out.println("Last pax sent, timer created, socket closed \n\n");
            writer.println(timeStamp()+": "+"Last packet in current window SENT. TIMER STARTED\n\n");
        } catch (SocketException ex) {
            Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void sendPacket(Packet packetObj, DatagramSocket sendSocket) {

        try {
            //check if EOT
            if (packetObj.getSeqNum() == pax) {
                //set type to EOT
                packetObj.setPacketType(3);
                writer.println(timeStamp()+": "+"SENT | #"+packetObj.getSeqNum()+" | "+paxType(packetObj.getPacketType())+" TO NETWORK");
                writer.println("--EOT HAS BEEN SENT BACK, CLOSING LOG --");
                    writer.close();
            }
            sendData = prepPacket(packetObj);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, networkAddr, networkPort);
            sendSocket.send(sendPacket);
            System.out.println("SENT | #"+packetObj.getSeqNum()+" | "+paxType(packetObj.getPacketType())+" TO NETWORK");
                writer.println(timeStamp()+": "+"SENT | #"+packetObj.getSeqNum()+" | "+paxType(packetObj.getPacketType())+" TO NETWORK");
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

                    System.out.println("RCVD | #"+packet.getSeqNum()+" | "+paxType(packet.getPacketType()));
                    writer.println("RCVD | #"+packet.getSeqNum()+" | "+paxType(packet.getPacketType()));

                    removeInWindow(packet.getSeqNum());

                    checkArray();

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

                    System.out.println("RCVD | #"+packet.getSeqNum()+" | "+paxType(packet.getPacketType()));
                    writer.println("RCVD | #"+packet.getSeqNum()+" | "+paxType(packet.getPacketType()));

                    //If the packet is not an EOT, 
                    if (packet.getPacketType() == 1) {

                        //Convert the packet into an ACK
                        packet.setPacketType(2);
                    }
                    else{
                        if (packet.getPacketType() == 3){
                            packet.setPacketType(5);
                        }
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
            writer.println(timeStamp()+": "+"Array empty. All Pax arrived. Timer Stop");
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
            System.out.println("\n POST TRANSMISSION STATS");
            writer.println(timeStamp()+": "+"POST TRANSMISSION STATS \n");
            System.out.println("LAST TRANSMISSION WINDOW SIZE:" + window);
            writer.println(timeStamp()+": "+"LAST TRANSMISSION WINDOW SIZE:" + window);
            System.out.println(HostB.packetsContainer.size() + "PACKETS LOST");
            writer.println(timeStamp()+": "+HostB.packetsContainer.size() + "PACKETS LOST");
            int success = window - packetsContainer.size();
            System.out.println(success+"/"+window+"packets received successfully");
             writer.println(timeStamp()+": "+success+"/"+window+"packets received successfully");
            System.out.println("Pausing for 5 seconds before sending again");
            writer.println(timeStamp()+": "+"Pausing for 5 seconds before sending again");
            Thread.sleep(5000);
           writer.println(timeStamp()+": "+"Sleep Finished! sending again");
            sendMode = true;
            HostB.SEND();

        } catch (InterruptedException ex) {
            Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static String paxType(int packetType){
      String type = null;
  switch (packetType){
      case 1: type = "PSH";
                break;
      case 2: type = "ACK";
                break;
      case 3: type = "EOT";
              break;
      case 4: type = "LOSS";
              break;  
  }
  
  return type;
  }
    
    public static String timeStamp(){
        return timeStamp = new SimpleDateFormat("yyyy,MM,dd_HH:mm:ss").format(Calendar.getInstance().getTime());
    }

}


