
import java.io.Serializable;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author User
 */
public class Packet implements Serializable {
    

    //1 = PSH 2=ACK 3=EOT 4=LOSS



    private int PacketType;
    private int seqNum;
    private int WindowSize;
    private int AckNum;
   
    
    public Packet(int pType, int sNum, int wSize, int aNum){
        this.PacketType = pType;
        this.seqNum = sNum;
        this.WindowSize = wSize;
        this.AckNum = aNum;
    };

    /**
     * @return the PacketType
     */
    public int getPacketType() {
        return PacketType;
    }

    /**
     * @param PacketType the PacketType to set
     */
    public void setPacketType(int PacketType) {
        this.PacketType = PacketType;
    }

    /**
     * @return the seqNum
     */
    public int getSeqNum() {
        return seqNum;
    }

    /**
     * @param seqNum the seqNum to set
     */
    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    /**
     * @return the WindowSize
     */
    public int getWindowSize() {
        return WindowSize;
    }

    /**
     * @param WindowSize the WindowSize to set
     */
    public void setWindowSize(int WindowSize) {
        this.WindowSize = WindowSize;
    }

    /**
     * @return the AckNum
     */
    public int getAckNum() {
        return AckNum;
    }

    /**
     * @param AckNum the AckNum to set
     */
    public void setAckNum(int AckNum) {
        this.AckNum = AckNum;
    }
    
    @Override
    public String toString(){
        return "seqNum= "+ this.getSeqNum() +"packetType= "+this.getPacketType();
    }
    
    
    
    
}
