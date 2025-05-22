package bhavya;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.data.gantt.*;
import org.jfree.ui.ApplicationFrame;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.*;

public class GanttChartDemo extends ApplicationFrame {
    public GanttChartDemo(String title) {
        super(title);
        TaskSeriesCollection dataset = createDataset();
        JFreeChart chart = ChartFactory.createGanttChart(
                "FCFS Gantt Chart", "VM", "Time", dataset, true, true, false);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setRenderer(new GanttRenderer());
        DateAxis axis = (DateAxis) plot.getRangeAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));

        ChartPanel panel = new ChartPanel(chart);
        setContentPane(panel);
    }

    private TaskSeriesCollection createDataset() {
        TaskSeries series = new TaskSeries("Scheduled Tasks");

        try (BufferedReader br = new BufferedReader(new FileReader("gantt_data.csv"))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                String cloudletId = tokens[0];
                String vmId = tokens[1];
                double start = Double.parseDouble(tokens[2]) * 1000;
                double end = Double.parseDouble(tokens[3]) * 1000;

                Date startDate = new Date((long) start);
                Date endDate = new Date((long) end);
                Task task = new Task("Cloudlet " + cloudletId, startDate, endDate);
                series.add(task);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        TaskSeriesCollection collection = new TaskSeriesCollection();
        collection.add(series);
        return collection;
    }

    public static void main(String[] args) {
        GanttChartDemo demo = new GanttChartDemo("Gantt Chart");
        demo.setSize(1000, 600);
        demo.setLocationRelativeTo(null);
        demo.setVisible(true);
    }
} 
