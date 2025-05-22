package bhavya;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import java.util.ArrayList;
public class FCFSDatacenterBroker extends DatacenterBroker {

  public FCFSDatacenterBroker(String name) throws Exception {
      super(name);
  }

 
  public void scheduleTaskstoVms() {

      ArrayList<Cloudlet> clist = new ArrayList<Cloudlet>();

      for (Cloudlet cloudlet : getCloudletSubmittedList()) {
          clist.add(cloudlet);
      }

      setCloudletReceivedList(clist);
  }


  protected void processCloudletReturn(SimEvent ev) {
      Cloudlet cloudlet = (Cloudlet) ev.getData();
      getCloudletReceivedList().add(cloudlet);
      Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
              + " received");
      cloudletsSubmitted--;
      if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) {
          scheduleTaskstoVms();
          cloudletExecution(cloudlet);
      }
  }


  protected void cloudletExecution(Cloudlet cloudlet) {

      if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { 
          Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
          clearDatacenters();
          finishExecution();
      } else { 
          if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {
              
              clearDatacenters();
              createVmsInDatacenter(0);
          }

      }
  }
}
