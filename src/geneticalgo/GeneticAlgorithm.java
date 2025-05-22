package geneticalgo;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import src.utils.Constants;
import src.utils.DatacenterCreator;
import src.utils.GenerateMatrices;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;

public class GeneticAlgorithm {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;
    private static Datacenter[] datacenter;
    private static double[][] commMatrix;
    private static double[][] execMatrix;

    private static final int POP_SIZE = 50;
    private static final int GENERATIONS = 100;
    private static final double CROSSOVER_RATE = 0.8;
    private static final double MUTATION_RATE = 0.1;

    public static void main(String[] args) {
        Log.printLine("Starting GA Scheduler...");

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

            GADatacenterBroker broker = new GADatacenterBroker("Broker_0");
            int brokerId = broker.getId();

            vmList = createVMs(brokerId, Constants.NO_OF_DATA_CENTERS);
            cloudletList = createCloudlets(brokerId, Constants.NO_OF_TASKS, 0);

            broker.submitVmList(vmList);

            List<Integer> bestChromosome = runGA();

           
            for (int i = 0; i < cloudletList.size(); i++) {
                cloudletList.get(i).setVmId(bestChromosome.get(i));
            }

            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();

            List<Cloudlet> resultList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            printCloudletList(resultList);
            saveGanttData(resultList);

            Log.printLine(GeneticAlgorithm.class.getName() + " finished!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Vm> createVMs(int userId, int count) {
        List<Vm> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Vm vm = new Vm(i, userId, 250, 1, 512, 1000, 10000, "Xen", new CloudletSchedulerSpaceShared());
            list.add(vm);
        }
        return list;
    }

    private static List<Cloudlet> createCloudlets(int userId, int count, int idShift) {
        List<Cloudlet> list = new ArrayList<>();
        UtilizationModel utilization = new UtilizationModelFull();

        for (int i = 0; i < count; i++) {
            int dcId = (int) (Math.random() * Constants.NO_OF_DATA_CENTERS);
            long len = (long) (1000 * (commMatrix[i][dcId] + execMatrix[i][dcId]));
            Cloudlet cl = new Cloudlet(idShift + i, len, 1, 300, 300, utilization, utilization, utilization);
            cl.setUserId(userId);
            list.add(cl);
        }
        return list;
    }

    // ========== GA Implementation ==========
    private static void saveGanttData(List<Cloudlet> list) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("ga_data.csv"));
            writer.println("CloudletID,VMID,StartTime,FinishTime");

           /* for (Cloudlet cloudlet : list) {
                if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                    writer.printf("%d,%d,%.2f,%.2f\n",
                            cloudlet.getCloudletId(),
                            cloudlet.getVmId(),
                            cloudlet.getExecStartTime(),
                            cloudlet.getFinishTime());
                    	
                }
            }*/
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
            Log.printLine("data saved to ga_data.csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Integer> runGA() {
        Random rand = new Random();
        List<List<Integer>> population = new ArrayList<>();

        // Initialize population
        for (int i = 0; i < POP_SIZE; i++) {
            List<Integer> chromo = new ArrayList<>();
            for (int j = 0; j < Constants.NO_OF_TASKS; j++) {
                chromo.add(rand.nextInt(Constants.NO_OF_DATA_CENTERS));
            }
            population.add(chromo);
        }

        List<Integer> best = null;
        double bestFitness = Double.MAX_VALUE;

        for (int gen = 0; gen < GENERATIONS; gen++) {
            List<List<Integer>> newPop = new ArrayList<>();

            for (int i = 0; i < POP_SIZE; i++) {
                List<Integer> parent1 = select(population);
                List<Integer> parent2 = select(population);

                List<Integer> child = crossover(parent1, parent2);
                mutate(child);

                newPop.add(child);

                double fit = fitness(child);
                if (fit < bestFitness) {
                    bestFitness = fit;
                    best = new ArrayList<>(child);
                }
            }
            population = newPop;
        }
        return best;
    }

    private static List<Integer> select(List<List<Integer>> population) {
        Random rand = new Random();
        List<Integer> a = population.get(rand.nextInt(POP_SIZE));
        List<Integer> b = population.get(rand.nextInt(POP_SIZE));
        return fitness(a) < fitness(b) ? a : b;
    }

    private static List<Integer> crossover(List<Integer> p1, List<Integer> p2) {
        Random rand = new Random();
        if (rand.nextDouble() > CROSSOVER_RATE) return new ArrayList<>(p1);

        int point = rand.nextInt(p1.size());
        List<Integer> child = new ArrayList<>(p1.subList(0, point));
        child.addAll(p2.subList(point, p2.size()));
        return child;
    }

    private static void mutate(List<Integer> chromo) {
        Random rand = new Random();
        for (int i = 0; i < chromo.size(); i++) {
            if (rand.nextDouble() < MUTATION_RATE) {
                chromo.set(i, rand.nextInt(Constants.NO_OF_DATA_CENTERS));
            }
        }
    }

    private static double fitness(List<Integer> chromo) {
        double[] finishTime = new double[Constants.NO_OF_DATA_CENTERS];
        for (int i = 0; i < chromo.size(); i++) {
            int vmId = chromo.get(i);
            finishTime[vmId] += execMatrix[i][vmId] + commMatrix[i][vmId];
        }

        double makespan = Arrays.stream(finishTime).max().orElse(0);
        return makespan;
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
        Log.printLine("Makespan using GA: " + dft.format(makespan));
        Log.printLine("Average Waiting Time: " + dft.format(avgWaitingTime));
        Log.printLine("Average Turnaround Time: " + dft.format(avgTurnaroundTime));
        Log.printLine("Throughput: " + String.format("%.4f", throughput) + " cloudlets/unit time");
    }
}
