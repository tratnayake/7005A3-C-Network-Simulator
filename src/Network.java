/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author thilinaratnayake
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Network {
    public static InetAddress HostAaddr;
    public static InetAddress HostBaddr;
    public static String controlMessage;
    public static int aControlPort = 5004;
    public static int bControlPort = 5005;
            

    public static void main(String args[]) throws Exception {
        
        //HostA InetAddress;
         HostAaddr = InetAddress.getByName("localhost");
        
        //HostB InetAddress
         HostBaddr = InetAddress.getByName("localhost");
        
        System.out.println("Welcome to the NETWORK for T and Elton's Assignment3");
        System.out.println("There are two hosts available for this simulation. Please choose which Host you would like to send from?");
        System.out.println("*NOTE* The host that isn't provided will be the receiver");
        Scanner scan = new Scanner(System.in);
        boolean loopCondition = true;
        while(loopCondition == true){
        String sender = scan.nextLine();
        switch (sender){
            case "a": 
                System.out.println("A will be sender");
                System.out.println("B will be receiver");
                controlMessage = "a";
                loopCondition = false;
                break;
            case "b":
                System.out.println("B will be sender");
                System.out.println("A will be receiver");
                controlMessage = "b";
                loopCondition = false;
                break;
            default: System.out.println("Please enter a valid command");
                break;
                
        }
        
    }
        
        
        System.out.println("Note that this is the percentage of drop rate for each packet");
        System.out.println("How many packets would you like to drop?");
        
        int bitErr = scan.nextInt();
        System.out.println("You have chosen to drop " + bitErr + " packets");
        sendControlSignal(controlMessage);
        System.out.println("Control messages sent to hosts \n BEGIN!");
        
        
        //Create a new thread that LISTENS to HOST A on 7005, and FORWARDS to B:8005
        HostThread HostA = new HostThread("HostA",7005,8005,bitErr);
        Thread HostAThread = new Thread(HostA);
        HostAThread.start();
        
        //Thread LISTENS to HOST B on 7006, FORWARDS to A: 8006
        HostThread HostB = new HostThread("HostB",7006,8006,bitErr);
        Thread HostBThread = new Thread(HostB);
        HostBThread.start();
        
        HostAThread.join();
        HostBThread.join();
        
        
    }
    
    public static void sendControlSignal(String sender){
        try {
            DatagramSocket aSocket = new DatagramSocket();
            DatagramSocket bSocket = new DatagramSocket();
            
            byte[] dataContainerA = new byte[8];
            byte[] dataContainerB = new byte[8];
            
            String aCommand;
            String bCommand;
            
            if (sender.equals("a")){
                aCommand = "1";
                bCommand ="2";
            }
            else{
                aCommand ="2";
                bCommand ="1";
            }
                    
            dataContainerA = aCommand.getBytes();
            dataContainerB = bCommand.getBytes();
            
            DatagramPacket aCommandPacket = new DatagramPacket(dataContainerA,dataContainerA.length,HostAaddr,aControlPort);
            DatagramPacket bCommandPacket = new DatagramPacket(dataContainerB, dataContainerB.length,HostBaddr,bControlPort);
            
            aSocket.send(aCommandPacket);
            bSocket.send(bCommandPacket);
           
            System.out.println("Command "+aCommand+" sent to A and command "+bCommand+" sent to B \n\n");
            
            
        } catch (SocketException ex) {
            Logger.getLogger(Network.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Network.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}

class HostThread implements Runnable {
    
   private Thread t;
   private String threadName;
   private int listenPort;
   private int forwardPort;
   private int drop;
   
   HostThread(String name, int listen, int forward, int droprate){
       threadName = name;
       listenPort = listen;
       forwardPort = forward;
       drop = droprate;
       
       System.out.println(name + "instantiated" );
   }
   public void run() {
       
        Random randomNum = new Random();

      try{
        DatagramSocket listenSocket = new DatagramSocket(listenPort);
        //change this shit later
        DatagramSocket forwardSocket = new DatagramSocket();
           
           byte[] receiveData =  new byte[1024];
           byte[] sendData = new byte[1024];
        while(true)
        {
             DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
               
               String received = new String(receivePacket.getData());
             
               listenSocket.receive(receivePacket);
               
               receiveData = receivePacket.getData();
               
               ByteArrayInputStream in = new ByteArrayInputStream(receiveData);
               ObjectInputStream is = new ObjectInputStream(in);
               
               try{
                   String rAddr = receivePacket.getSocketAddress().toString();
                   Packet packet = (Packet) is.readObject();
                   //packet.toString();
                   System.out.println("RX | #"+packet.getSeqNum()+" | "+paxType(packet.getPacketType())+" FROM "+ rAddr);
                   
                   //Packets will be dropped randomly when bit error rate is less than or equal to the rndom nmber generator        
                    if(drop >= 1 && drop <= 100)
                    {
                        int randomInt = randomNum.nextInt(100) + 1;
                        if (randomInt <= drop)
                        {
                            
                            System.out.println("\n\n**DROP PACKET "+packet.getSeqNum()+"**\n\n");    
                            continue;
                            
                        }
                    }
                    else
                    {
                        System.out.println("The maximum is 100, try again");
                    }
                   
                    //Forward to second host
                  
                   //SERIALIZE packet back down into byte stream
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ObjectOutputStream os = new ObjectOutputStream(outputStream);
                    os.writeObject(packet); 
                    sendData = outputStream.toByteArray();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost") ,forwardPort );
                    forwardSocket.send(sendPacket);
                    //System.out.println("TX to "+forwardSocket.getInetAddress());
                     System.out.println("TX | #"+packet.getSeqNum()+" | "+paxType(packet.getPacketType())+" TO  "+forwardSocket.getInetAddress() );
               }
               catch(Exception e){
                   e.printStackTrace();
               }
        }
        }
        catch (IOException e)
        {
            System.out.println("io exception");
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

}
