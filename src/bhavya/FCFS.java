package bhavya;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import src.utils.Constants;
import src.utils.DatacenterCreator;
import src.utils.GenerateMatrices;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.io.FileWriter;
import java.io.PrintWriter;

public class FCFS {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;
    private static Datacenter[] datacenter;
    private static double[][] commMatrix;
    private static double[][] execMatrix;

    private static List<Vm> createVM(int userId, int vms) {
        LinkedList<Vm> list = new LinkedList<Vm>();

        long size = 10000;
        int ram = 512;
        int mips = 250;
        long bw = 1000;
        int pesNumber = 1;
        String vmm = "Xen";

        Vm[] vm = new Vm[vms];

        for (int i = 0; i < vms; i++) {
            vm[i] = new Vm(datacenter[i].getId(), userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }

        return list;
    }

    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet[] cloudlet = new Cloudlet[cloudlets];

        for (int i = 0; i < cloudlets; i++) {
            int dcId = (int) (Math.random() * Constants.NO_OF_DATA_CENTERS);
            long length = (long) (1e3 * (commMatrix[i][dcId] + execMatrix[i][dcId]));
            cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet[i].setUserId(userId);
            cloudlet[i].setVmId(dcId + 2);
            list.add(cloudlet[i]);
        }
        return list;
    }

    public static void main(String[] args) {
        Log.printLine("Starting FCFS Scheduler...");

        new GenerateMatrices();
        execMatrix = GenerateMatrices.getExecMatrix();
        commMatrix = GenerateMatrices.getCommMatrix();

        try {
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            datacenter = new Datacenter[Constants.NO_OF_DATA_CENTERS];
            for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
                datacenter[i] = DatacenterCreator.createDatacenter("Datacenter_" + i);
            }

            FCFSDatacenterBroker broker = createBroker("Broker_0");
            int brokerId = broker.getId();

            vmList = createVM(brokerId, Constants.NO_OF_DATA_CENTERS);
            cloudletList = createCloudlet(brokerId, Constants.NO_OF_TASKS, 0);

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            printCloudletList(newList);
            saveGanttData(newList); 

            Log.printLine(FCFS.class.getName() + " finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static FCFSDatacenterBroker createBroker(String name) throws Exception {
        return new FCFSDatacenterBroker(name);
    }

    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" +
                indent + "Data center ID" +
                indent + "VM ID" +
                indent + "Time" +
                indent + "Start Time" +
                indent + "Finish Time" +
                indent + "Waiting Time" +
                indent + "Turnaround Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        dft.setMinimumIntegerDigits(2);

        double totalTurnaroundTime = 0;
        double totalWaitingTime = 0;
        double lastFinishTime = 0;
        int totalCompleted = 0;

        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                double turnaroundTime = cloudlet.getFinishTime();
                double waitingTime = turnaroundTime - cloudlet.getActualCPUTime();

                totalTurnaroundTime += turnaroundTime;
                totalWaitingTime += waitingTime;
                lastFinishTime = Math.max(lastFinishTime, cloudlet.getFinishTime());
                totalCompleted++;

                Log.print(indent + dft.format(cloudlet.getCloudletId()) + indent + indent);
                Log.print("SUCCESS");
                Log.printLine(indent + indent + dft.format(cloudlet.getResourceId()) +
                        indent + indent + dft.format(cloudlet.getVmId()) +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + dft.format(cloudlet.getFinishTime()) +
                        indent + indent + dft.format(waitingTime) +
                        indent + indent + dft.format(turnaroundTime));
            }
        }

        double makespan = calcMakespan(list);
        double avgWaitingTime = totalWaitingTime / totalCompleted;
        double avgTurnaroundTime = totalTurnaroundTime / totalCompleted;
        double throughput = (double) totalCompleted / lastFinishTime;

        Log.printLine("\n=== PERFORMANCE METRICS ===");
        Log.printLine("Makespan using FCFS: " + makespan);
        Log.printLine("Average Waiting Time: " + String.format("%.2f", avgWaitingTime));
        Log.printLine("Average Turnaround Time: " + String.format("%.2f", avgTurnaroundTime));
        Log.printLine("Throughput: " + String.format("%.4f", throughput) + " cloudlets/unit time");
    }

    private static double calcMakespan(List<Cloudlet> list) {
        double makespan = 0;
        double[] dcWorkingTime = new double[Constants.NO_OF_DATA_CENTERS];

        for (int i = 0; i < Constants.NO_OF_TASKS; i++) {
            int dcId = list.get(i).getVmId() % Constants.NO_OF_DATA_CENTERS;
            if (dcWorkingTime[dcId] != 0) --dcWorkingTime[dcId];
            dcWorkingTime[dcId] += execMatrix[i][dcId] + commMatrix[i][dcId];
            makespan = Math.max(makespan, dcWorkingTime[dcId]);
        }
        return makespan;
    }

    private static void saveGanttData(List<Cloudlet> list) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("gantt_data.csv"));
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
            Log.printLine("data saved to gantt_data.csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
