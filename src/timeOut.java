
import java.util.TimerTask;

abstract class timeOut extends TimerTask {
    
    public String Host;

       public timeOut(String Host){
           this.Host = Host;
       }
    public void run() {
        System.out.println("TIME OUT FOR HOST " +Host);
        
        if (Host.equals("A")){
        HostA.TIMEOUT();
        //If all other packets in the array are LOSS, then that means     
        }
        else{
            HostB.TIMEOUT();
        }
        

    }

}