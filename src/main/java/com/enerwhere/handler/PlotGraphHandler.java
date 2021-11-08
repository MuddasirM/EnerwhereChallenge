package com.enerwhere.handler;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class PlotGraphHandler extends JPanel {

	protected List<Float> pointsToPlot = null;

	public PlotGraphHandler(List<Float> pointsToPlot) {
		super();
		this.pointsToPlot = pointsToPlot;
	}

	int marginPadding = 30;
	private static final int Y_HATCH_CNT = 100;
	private static final int GRAPH_POINT_WIDTH = 3;

	// This method will take the odered lists and calculate the area under the curve
	// of active power per hour to get the energy produced on that day and using the
	// energy produced will calculate the efficiency of the plant and display it in
	// the graph title
	public void plotGraph(List<Float> oderedPowerValues, Integer day, float litersConsumed) {
		pointsToPlot = oderedPowerValues;

		float powerProduced = calculateEnergyProducedPerDay(oderedPowerValues, 1);
		float plantEfficency = calculateEfficency(powerProduced, litersConsumed);
		JFrame frame = new JFrame();
		frame.setTitle("Day " + day + " power produced " + powerProduced + "kWh plant efficency today " + plantEfficency
				+ "%");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new PlotGraphHandler(oderedPowerValues));
		frame.setSize(500, 700);
		frame.setLocation(200, 200);
		frame.setVisible(true);

	}

	private float calculateEfficency(float powerProduced, float litersConsumed) {
		float plantEfficency = (litersConsumed / powerProduced) * 100;
		return plantEfficency;
	}

	private float calculateEnergyProducedPerDay(List<Float> oderedPowerValues, int xDist) {
		if (oderedPowerValues.size() == 1 || oderedPowerValues.size() == 0)
			return 0f;
		float integral = 0;
		float prev = oderedPowerValues.get(0);
		for (int i = 1; i < oderedPowerValues.size(); i++) {
			integral += xDist * (prev + oderedPowerValues.get(i)) / 2.0;
			prev = oderedPowerValues.get(i);
		}
		return integral;
	}

	protected void paintComponent(Graphics grf) {
		// create instance of the Graphics to use its methods
		super.paintComponent(grf);
		Graphics2D graph = (Graphics2D) grf;

		// Sets the value of a single preference for the rendering algorithms.
		graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// get width and height
		int width = getWidth();
		int height = getHeight();

		// draw graph
		graph.draw(new Line2D.Double(marginPadding, marginPadding, marginPadding, height - marginPadding));
		graph.draw(new Line2D.Double(marginPadding, height - marginPadding, width - marginPadding,
				height - marginPadding));

		// find value of x and scale to plot points
		double x = (double) (width - 2 * marginPadding) / (pointsToPlot.size() - 1);
		double scale = (double) (height - 2 * marginPadding) / getMax();

		// create hatch marks for y axis.
		for (int i = 0; i < Y_HATCH_CNT; i++) {
			int x0 = marginPadding;
			int x1 = GRAPH_POINT_WIDTH + marginPadding;
			int y0 = getHeight() - (((i + 1) * (getHeight() - marginPadding * 2)) / Y_HATCH_CNT + marginPadding);
			int y1 = y0;
			graph.drawLine(x0, y0, x1, y1);
		}

		// and for x axis
		for (int i = 0; i < pointsToPlot.size() - 1; i++) {
			int x0 = (i + 1) * (getWidth() - marginPadding * 2) / (pointsToPlot.size() - 1) + marginPadding;
			int x1 = x0;
			int y0 = getHeight() - marginPadding;
			int y1 = y0 - GRAPH_POINT_WIDTH;
			graph.drawLine(x0, y0, x1, y1);
		}

		// set color for points
		graph.setPaint(Color.BLUE);
		graph.setStroke(new BasicStroke(1f));
		// set points to the graph
		for (int i = 0; i < pointsToPlot.size(); i++) {
			double x1 = marginPadding + i * x;
			double y1 = height - marginPadding - scale * pointsToPlot.get(i);
			int x2 = (int) x1;
			int y2 = (int) y1;
			if (i != 0) {
				x2 = (int) (marginPadding + (i - 1) * x);
				y2 = (int) (height - marginPadding - scale * pointsToPlot.get(i - 1));
			}
			graph.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
			graph.drawLine((int) x1, (int) y1, x2, y2);
		}

	}

	// create getMax() method to find maximum value
	private float getMax() {
		float max = -Integer.MAX_VALUE;
		for (int i = 0; i < pointsToPlot.size(); i++) {
			if (pointsToPlot.get(i) > max)
				max = pointsToPlot.get(i);
		}
		return max;
	}

}
