package SJF;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import src.utils.Constants;
import src.utils.DatacenterCreator;
import src.utils.GenerateMatrices;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;

public class SJF_Scheduler {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;
    private static Datacenter[] datacenter;
    private static double[][] commMatrix;
    private static double[][] execMatrix;

    private static List<Vm> createVM(int userId, int vms) {
        LinkedList<Vm> list = new LinkedList<>();

        // VM Parameters
        long size = 10000; // image size (MB)
        int ram = 512; // VM memory (MB)
        int mips = 250;
        long bw = 1000;
        int pesNumber = 1; // number of CPUs
        String vmm = "Xen"; // VMM name

        // create VMs
        for (int i = 0; i < vms; i++) {
            Vm vm = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm);
        }

        return list;
    }

    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
        LinkedList<Cloudlet> list = new LinkedList<>();

        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        for (int i = 0; i < cloudlets; i++) {
            int dcId = (int) (Math.random() * Constants.NO_OF_DATA_CENTERS);
            long length = (long) (1e3 * (commMatrix[i][dcId] + execMatrix[i][dcId]));
            Cloudlet cloudlet = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize,
                    utilizationModel, utilizationModel, utilizationModel);
            cloudlet.setUserId(userId);
            cloudlet.setVmId(dcId); // Assigning VM ID within the range
            list.add(cloudlet);
        }

        return list;
    }
    private static void saveGanttData(List<Cloudlet> list) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("SJF_data.csv"));
            writer.println("CloudletID,VMID,StartTime,FinishTime");
            for (Cloudlet cloudlet : list) {
                if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                    double start = cloudlet.getExecStartTime();
                    double finish = cloudlet.getFinishTime();
                    double actualCpuTime = cloudlet.getActualCPUTime();
                    double turnaroundTime = finish;
                    double waitingTime = turnaroundTime - actualCpuTime;

                    writer.printf("%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                            cloudlet.getCloudletId(),
                            cloudlet.getVmId(),
                            start,
                            finish,
                            actualCpuTime,
                            turnaroundTime,
                            waitingTime);
                }
            }

            writer.close();
            Log.printLine("data saved to SJF_data.csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        Log.printLine("Starting SJF Scheduler...");

        new GenerateMatrices();
        execMatrix = GenerateMatrices.getExecMatrix();
        commMatrix = GenerateMatrices.getCommMatrix();

        try {
            int num_user = 1; // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            // Create Datacenters
            datacenter = new Datacenter[Constants.NO_OF_DATA_CENTERS];
            for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
                datacenter[i] = DatacenterCreator.createDatacenter("Datacenter_" + i);
            }

            // Create Broker
            SJF_DatacenterBroker broker = createBroker("Broker_0");
            int brokerId = broker.getId();

            // Create VMs and Cloudlets and send them to broker
            vmList = createVM(brokerId, Constants.NO_OF_DATA_CENTERS);
            cloudletList = createCloudlet(brokerId, Constants.NO_OF_TASKS, 0);

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            // Start the simulation
            CloudSim.startSimulation();

            // Retrieve results when simulation is over
            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            printCloudletList(newList);

            Log.printLine(SJF_Scheduler.class.getName() + " finished!");
            saveGanttData(newList);
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static SJF_DatacenterBroker createBroker(String name) throws Exception {
        return new SJF_DatacenterBroker(name);
    }

   /* private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" +
                indent + "Data center ID" +
                indent + "VM ID" +
                indent + indent + "Time" +
                indent + "Start Time" +
                indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        dft.setMinimumIntegerDigits(2);
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + dft.format(cloudlet.getCloudletId()) + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");

                Log.printLine(indent + indent + dft.format(cloudlet.getResourceId()) +
                        indent + indent + indent + dft.format(cloudlet.getVmId()) +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }
        double makespan = calcMakespan(list);
        Log.printLine("Makespan using SJF: " + makespan);
    }
*/
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
        Log.printLine("Makespan using GA: " + dft.format(makespan));
        Log.printLine("Average Waiting Time: " + dft.format(avgWaitingTime));
        Log.printLine("Average Turnaround Time: " + dft.format(avgTurnaroundTime));
        Log.printLine("Throughput: " + String.format("%.4f", throughput) + " cloudlets/unit time");
    }
    private static double calcMakespan(List<Cloudlet> list) {
        double makespan = 0;
        double[] dcWorkingTime = new double[Constants.NO_OF_DATA_CENTERS];

        for (int i = 0; i < Constants.NO_OF_TASKS; i++) {
            int dcId = list.get(i).getVmId() % Constants.NO_OF_DATA_CENTERS;
            dcWorkingTime[dcId] += execMatrix[i][dcId] + commMatrix[i][dcId];
            makespan = Math.max(makespan, dcWorkingTime[dcId]);
        }
        return makespan;
    }
}
