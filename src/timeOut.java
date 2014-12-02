
import java.util.TimerTask;

abstract class timeOut extends TimerTask {
    
    public String Host;

    //check for host
       public timeOut(String Host){
           this.Host = Host;
       }
       /*
       *Timeout if host is A
       *Timeout if host is B
       */
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