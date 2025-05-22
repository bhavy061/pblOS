package geneticalgo;
/*import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.ui.ApplicationFrame;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SchedulerVisualization extends ApplicationFrame {

   public SchedulerVisualization(String title) {
        super(title);
        TaskSeriesCollection dataset = createDataset("ga_data.csv");
        JFreeChart chart = ChartFactory.createGanttChart(
                "Genetic Algorithm Scheduler Gantt Chart",
                "VM ID",
                "Time",
                dataset,
                true,
                true,
                false
        );

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        GanttRenderer renderer = (GanttRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(true);

        DateAxis axis = (DateAxis) plot.getRangeAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss.SSS"));

        ChartPanel panel = new ChartPanel(chart);
        setContentPane(panel);
    }


    private TaskSeriesCollection createDataset(String csvPath) {
        TaskSeriesCollection dataset = new TaskSeriesCollection();
        TaskSeries series = new TaskSeries("Scheduled Tasks");

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                int cloudletId = Integer.parseInt(parts[0]);
                String vmId = "VM-" + parts[1];
                double start = Double.parseDouble(parts[2]);
                double end = Double.parseDouble(parts[3]);

                // Convert start and end to Date objects using offset (epoch origin)
                long startMillis = (long) (start * 1000);
                long endMillis = (long) (end * 1000);
                Date startDate = new Date(startMillis);
                Date endDate = new Date(endMillis);

                Task task = new Task("Cloudlet-" + cloudletId, startDate, endDate);

                Task parentTask = series.get(vmId);
                if (parentTask == null) {
                    parentTask = new Task(vmId, startDate, endDate);
                    parentTask.addSubtask(task);
                    series.add(parentTask);
                } else {
                    parentTask.addSubtask(task);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        dataset.add(series);
        return dataset;
    }

    public static void main(String[] args) {
        SchedulerVisualization chart = new SchedulerVisualization("Genetic Algorithm Task Scheduling");
        chart.setSize(1000, 600);
        chart.setLocationRelativeTo(null);
        chart.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        chart.setVisible(true);
    }
}
/*
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
class Process {
    String name;
    int startTime;
    int burstTime;

    public Process(String name, int startTime, int burstTime) {
        this.name = name;
        this.startTime = startTime;
        this.burstTime = burstTime;
    }
}

public class SchedulerVisualization extends JPanel {
    private final List<Process> processes;

    public SchedulerVisualization(List<Process> processes) {
        this.processes = processes;
        setPreferredSize(new Dimension(800, 200));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int x = 50; // initial X offset
        int y = 50;
        int height = 50;
        int scale = 40; // pixels per unit time

        // Draw each process
        for (Process p : processes) {
            int width = p.burstTime * scale;

            g.setColor(Color.CYAN);
            g.fillRect(x, y, width, height);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, width, height);

            // Draw process name centered
            g.drawString(p.name, x + width / 2 - 10, y + height / 2);

            // Draw time markers
            g.drawString(String.valueOf(p.startTime), x, y + height + 15);

            x += width;
        }

        // Draw final time
        if (!processes.isEmpty()) {
            Process last = processes.get(processes.size() - 1);
            int endTime = last.startTime + last.burstTime;
            g.drawString(String.valueOf(endTime), x, y + height + 15);
        }
    }

    public static void main(String[] args) {
        // Example process list (you can take input dynamically as well)
        List<Process> processList = new ArrayList<>();
        processList.add(new Process("P1", 0, 4));
        processList.add(new Process("P2", 4, 3));
        processList.add(new Process("P3", 7, 2));
        processList.add(new Process("P4", 9, 1));

        JFrame frame = new JFrame("Gantt Chart Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new SchedulerVisualization(processList));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
*/
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

public class SchedulerVisualization {

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
        String csvFile = "ga_data.csv";  // Fixed filename here
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
