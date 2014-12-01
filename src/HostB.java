
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
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
   
    public static byte[] sendData = new byte[1048];
   
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
   
   public static ArrayList<Packet> packetsContainer;
    
    public static void main(String args[]) throws Exception {
        
        pax = 15;
        window = 5;
        //START SEQUENCE NUMBERS FROM 1!
        seqNum = 1;
        
        networkAddr = InetAddress.getByName("localhost");
        networkPort = 7006;
        
        listenPort = 8005;
        
        //New socket purely meant for listening to commands from NET
        DatagramSocket commandSocket = new DatagramSocket(5005);  
        int startCommand = listenForCommand(commandSocket);
        System.out.println("Start Command is "+startCommand);
        
        //Create new packetsContainer to hold packets (this is essentially our window)
        packetsContainer = new ArrayList<>();
        
        if (startCommand == 1){
            sendMode = true;
            System.out.println("Go into SENDER mode");
        }
        else{
            sendMode = false;
            System.out.println("Go into RECEIVER mode");
            
        }
        
        
        //SEND();
        //Infinite loop
        while(true){
            
            //While in SENDMODE do SEND, when you're done SENDING, set to false and BREAK to go into RECEIVEACK mode
            while(sendMode){
                SEND();
            }
            RECEIVEACK();
        }
        
    }
    
    public static int listenForCommand(DatagramSocket commandSocket){
        int command = 0;
        while(true){
            try {
                System.out.println("Host B waiting for a command from NETWORK....");
                DatagramPacket receiveCommandPacket = new DatagramPacket(receiveCommandContainer,receiveCommandContainer.length);
                commandSocket.receive(receiveCommandPacket);
                String receivedCommand = new String(receiveCommandPacket.getData());
                receivedCommand = receivedCommand.trim();
                System.out.println("RECEIVED NESSAGE FROM NETWORK: "+receivedCommand);
                command = Integer.valueOf(receivedCommand);
                break;
            } catch (IOException ex) {
                Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return command;
    }
    
    public static void SEND(){
        
          //1. Scenario: Starting from seqNum 1 -> window, add a packet into packet Container    
        for (int i = 1; i <= window ; i++) {
            //Create the packets to send
            Packet packet = new Packet(1, seqNum, 5, seqNum);
            //Add those packets to the container (window)
            packetsContainer.add(packet);
            //System.out.println("Packet created for " + i);
            System.out.println("Packet #"+packet.getSeqNum()+" added to container");     
           //Increase the sequence number
            seqNum ++;
        }
        
        //Send those packets
            sendPackets(packetsContainer);
        //Send those pax
        
        //go into receiveMode
        sendMode = false;
       
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
    
    public static void sendPackets(ArrayList<Packet> packetContainer){
       try {
           DatagramSocket sendSocket = new DatagramSocket();
           for (Packet packetObj: packetContainer) {
               sendData = prepPacket(packetObj);
               DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,networkAddr,networkPort);
               sendSocket.send(sendPacket);
               System.out.println("Packet #"+packetObj.getSeqNum()+" SENT!");
               sendSocket.close();
                       
                       }
           sendSocket.close();
       } catch (SocketException ex) {
           Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
       } catch (IOException ex) {
           Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
       }
    }
    
    public static void sendPacket(Packet packetObj, DatagramSocket sendSocket){
               
       try {
           
           sendData = prepPacket(packetObj);
           DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,networkAddr,networkPort);
           sendSocket.send(sendPacket);
           System.out.println("Packet #"+packetObj.getSeqNum()+" SENT!");
       } catch (SocketException ex) {
           Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
       } catch (IOException ex) {
           Logger.getLogger(HostB.class.getName()).log(Level.SEVERE, null, ex);
       }
                       
                      
    }
    
    public static void RECEIVE(){
        
       try {
           System.out.println("Inside receiveAck");
           byte[] receiveData = new byte[1024];
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
    
    public static void RECEIVEACK(){
        System.out.println("IN RECEIVE MODE");
       try {
           System.out.println("Inside receiveAck");
           byte[] receiveData = new byte[1024];
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
                   if(packet.getPacketType() != 3){
                       
                   //Convert the packet into an ACK
                   packet.setPacketType(2);
                   }
                   
                   
                   DatagramSocket sendSocket =new DatagramSocket();
                   sendPacket(packet,sendSocket);
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
    

}
