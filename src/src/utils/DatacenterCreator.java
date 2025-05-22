package src.utils;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DatacenterCreator {

    public static Datacenter createDatacenter(String name) {

        List<Host> hostList = new ArrayList<Host>();

    
        List<Pe> peList = new ArrayList<Pe>();

        int mips = 1000;

     
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        
        int hostId = 0;
        int ram = 2048; //host memory (MB)
        long storage = 1000000; //host storage
        int bw = 10000;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList,
                        new VmSchedulerTimeShared(peList)
                )
        ); 


       
        String arch = "x86";     
        String os = "Linux";          
        String vmm = "Xen";
        double time_zone = 10.0;        
        double cost = 3.0;           
        double costPerMem = 0.05;       
        double costPerStorage = 0.1;   
        double costPerBw = 0.1;            
        LinkedList<Storage> storageList = new LinkedList<Storage>();    

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

       
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datacenter;
    }
}
