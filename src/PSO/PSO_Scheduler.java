package PSO;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
//import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
//import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
//import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import src.utils.Constants;
import src.utils.DatacenterCreator;
import src.utils.GenerateMatrices;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;
import PSO.pso;

public class PSO_Scheduler {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;
    private static Datacenter[] datacenter;
    private static  pso PSOSchedularInstance;
    private static double mapping[];
    private static double[][] commMatrix;
    private static double[][] execMatrix;

    private static List<Vm> createVM(int userId, int vms) {
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<Vm> list = new LinkedList<Vm>();

        //VM Parameters
        long size = 10000; //image size (MB)
        int ram = 512; //vm memory (MB)
        int mips = 250;
        long bw = 1000;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name

        //create VMs
        Vm[] vm = new Vm[vms];

        for (int i = 0; i < vms; i++) {
            vm[i] = new Vm(datacenter[i].getId(), userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }

        return list;
    }

    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

        //cloudlet parameters
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet[] cloudlet = new Cloudlet[cloudlets];

        for (int i = 0; i < cloudlets; i++) {
            int dcId = (int) (mapping[i]);
            long length = (long) (1e3 * (commMatrix[i][dcId] + execMatrix[i][dcId]));
            cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet[i].setUserId(userId);
            list.add(cloudlet[i]);
        }

        return list;
    }
    private static void saveGanttData(List<Cloudlet> list) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("pso_data.csv"));
            writer.println("CloudletID,VMID,StartTime,FinishTime");

            for (Cloudlet cloudlet : list) {
                if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                    writer.printf("%d,%d,%.2f,%.2f\n",
                            cloudlet.getCloudletId(),
                            cloudlet.getVmId(),
                            cloudlet.getExecStartTime(),
                            cloudlet.getFinishTime());
                }
            }

            writer.close();
            Log.printLine("data saved to pso_data.csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Log.printLine("Starting PSO Scheduler...");

        new GenerateMatrices();
        commMatrix = GenerateMatrices.getCommMatrix();
        execMatrix = GenerateMatrices.getExecMatrix();
        PSOSchedularInstance = new pso();
        mapping = PSOSchedularInstance.run();

        try {
            int num_user = 1;   // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            // Second step: Create Datacenters
            datacenter = new Datacenter[Constants.NO_OF_DATA_CENTERS];
            for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
                datacenter[i] = DatacenterCreator.createDatacenter("Datacenter_" + i);
            }

            //Third step: Create Broker
            PSODatacenterBroker broker = createBroker("Broker_0");
            int brokerId = broker.getId();

            //Fourth step: Create VMs and Cloudlets and send them to broker
            vmList = createVM(brokerId, Constants.NO_OF_DATA_CENTERS);
            cloudletList = createCloudlet(brokerId, Constants.NO_OF_TASKS, 0);

            // mapping our dcIds to cloudsim dcIds
            HashSet<Integer> dcIds = new HashSet<>();
            HashMap<Integer, Integer> hm = new HashMap<>();
            for (Datacenter dc : datacenter) {
                if (!dcIds.contains(dc.getId()))
                    dcIds.add(dc.getId());
            }
            Iterator<Integer> it = dcIds.iterator();
            for (int i = 0; i < mapping.length; i++) {
                if (hm.containsKey((int) mapping[i])) continue;
                hm.put((int) mapping[i], it.next());
            }
            for (int i = 0; i < mapping.length; i++)
                mapping[i] = hm.containsKey((int) mapping[i]) ? hm.get((int) mapping[i]) : mapping[i];

            broker.submitVmList(vmList);
            broker.setMapping(mapping);
            broker.submitCloudletList(cloudletList);


            // Fifth step: Starts the simulation
            CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            printCloudletList(newList);
            saveGanttData(newList);

            Log.printLine(PSO_Scheduler.class.getName() + " finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static PSODatacenterBroker createBroker(String name) throws Exception {
        return new PSODatacenterBroker(name);
    }

    private static void printCloudletList(List<Cloudlet> list) {
        DecimalFormat dft = new DecimalFormat("###.##");

        double totalTurnaroundTime = 0;
        double totalWaitingTime = 0;
        double lastFinishTime = 0;
        int totalCompleted = 0;

        Log.printLine("Cloudlet ID\tStatus\tDatacenter ID\tVM ID\tTime\tStart Time\tFinish Time\tWaiting Time\tTurnaround Time");

        for (Cloudlet cl : list) {
            if (cl.getCloudletStatus() == Cloudlet.SUCCESS) {
                double turnaround = cl.getFinishTime();
                double waiting = turnaround - cl.getActualCPUTime();

                totalTurnaroundTime += turnaround;
                totalWaitingTime += waiting;
                lastFinishTime = Math.max(lastFinishTime, cl.getFinishTime());
                totalCompleted++;

                Log.printLine(cl.getCloudletId() + "\tSUCCESS\t" +
                        cl.getResourceId() + "\t" +
                        cl.getVmId() + "\t" +
                        dft.format(cl.getActualCPUTime()) + "\t" +
                        dft.format(cl.getExecStartTime()) + "\t" +
                        dft.format(cl.getFinishTime()) + "\t" +
                        dft.format(waiting) + "\t" +
                        dft.format(turnaround));
            }
        }

        double makespan = lastFinishTime;
        double avgWaitingTime = totalWaitingTime / totalCompleted;
        double avgTurnaroundTime = totalTurnaroundTime / totalCompleted;
        double throughput = totalCompleted / makespan;

        Log.printLine("\n=== PERFORMANCE METRICS ===");
        Log.printLine("Makespan using pso: " + dft.format(makespan));
        Log.printLine("Average Waiting Time: " + dft.format(avgWaitingTime));
        Log.printLine("Average Turnaround Time: " + dft.format(avgTurnaroundTime));
        Log.printLine("Throughput: " + String.format("%.4f", throughput) + " cloudlets/unit time");
        PSOSchedularInstance.printBestFitness();
    }
}