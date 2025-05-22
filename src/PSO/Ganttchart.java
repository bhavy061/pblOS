package PSO;

import org.jfree.chart.ChartFactory;
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

public class Ganttchart extends ApplicationFrame {

    public Ganttchart(String title) {
        super(title);
        TaskSeriesCollection dataset = createDataset("pso_data.csv");
        JFreeChart chart = ChartFactory.createGanttChart(
                "PSO Scheduler Gantt Chart",
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
    	Ganttchart chart = new Ganttchart("PSO Algorithm Task Scheduling");
        chart.setSize(1000, 600);
        chart.setLocationRelativeTo(null);
        chart.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        chart.setVisible(true);
    }
}

