package RoundRobin;
import java.io.*;
import java.util.*;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.data.gantt.*;

public class Ganttchart {

    static class CloudletTask {
        String cloudletID;
        int vmID;
        double startTime;
        double finishTime;
        double turnaroundTime;
        double waitingTime;

        CloudletTask(String cloudletID, int vmID, double startTime, double finishTime,
                    double turnaroundTime, double waitingTime) {
            this.cloudletID = cloudletID;
            this.vmID = vmID;
            this.startTime = startTime;
            this.finishTime = finishTime;
            this.turnaroundTime = turnaroundTime;
            this.waitingTime = waitingTime;
        }
    }

    public static void main(String[] args) {
        String csvFile = "roundrobin_data.csv";  // Fixed filename here
        List<CloudletTask> tasks = new ArrayList<>();

        // Read CSV file ga_data.csv
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String header = br.readLine();
            if (header == null) {
                System.out.println("CSV file is empty");
                return;
            }
            String[] columns = header.split(",");
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < columns.length; i++) {
                colIndex.put(columns[i].trim(), i);
            }

            if (!colIndex.containsKey("CloudletID") || !colIndex.containsKey("VMID") ||
                !colIndex.containsKey("StartTime") || !colIndex.containsKey("FinishTime")) {
                System.out.println("CSV missing required columns");
                return;
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < columns.length) continue;

                String cloudletID = parts[colIndex.get("CloudletID")].trim();
                int vmID = Integer.parseInt(parts[colIndex.get("VMID")].trim());
                double startTime = Double.parseDouble(parts[colIndex.get("StartTime")].trim());
                double finishTime = Double.parseDouble(parts[colIndex.get("FinishTime")].trim());

                double turnaroundTime = 0;
                double waitingTime = 0;

                if (colIndex.containsKey("TurnaroundTime")) {
                    turnaroundTime = Double.parseDouble(parts[colIndex.get("TurnaroundTime")].trim());
                } else if (colIndex.containsKey("ArrivalTime")) {
                    double arrivalTime = Double.parseDouble(parts[colIndex.get("ArrivalTime")].trim());
                    turnaroundTime = finishTime - arrivalTime;
                } else {
                    turnaroundTime = finishTime - startTime;
                }

                if (colIndex.containsKey("WaitingTime")) {
                    waitingTime = Double.parseDouble(parts[colIndex.get("WaitingTime")].trim());
                } else {
                    waitingTime = turnaroundTime - (finishTime - startTime);
                }

                tasks.add(new CloudletTask(cloudletID, vmID, startTime, finishTime, turnaroundTime, waitingTime));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        TaskSeries series = new TaskSeries("Cloudlets");
        for (CloudletTask t : tasks) {
            Date start = new Date((long)(t.startTime * 1000));
            Date end = new Date((long)(t.finishTime * 1000));
            Task task = new Task("Cloudlet " + t.cloudletID, start, end);
            series.add(task);
        }
        TaskSeriesCollection dataset = new TaskSeriesCollection();
        dataset.add(series);

        JFreeChart chart = ChartFactory.createGanttChart(
            "Gantt Chart - Cloudlet Execution using Genetic Algorithm",
            "Cloudlets",
            "Time (seconds)",
            dataset,
            true,
            true,
            false
        );

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        GanttRenderer renderer = (GanttRenderer) plot.getRenderer();

        Color[] colors = {
            Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA,
            Color.CYAN, Color.PINK, Color.YELLOW.darker(), Color.GRAY, Color.BLACK
        };

        // You can customize coloring here if you want

        DateAxis rangeAxis = (DateAxis) plot.getRangeAxis();
        rangeAxis.setDateFormatOverride(new SimpleDateFormat("s"));

        ChartFrame frame = new ChartFrame("Gantt Chart", chart);
        frame.setSize(1000, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    }
}
