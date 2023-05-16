package com.slimiot.hrv.Home;

import java.util.ArrayList;

public class HRVCalculator {

    private static final double ULF_MIN_FREQ = 0.0;
    private static final double ULF_MAX_FREQ = 0.0033;
    private static final double VLF_MIN_FREQ = 0.0033;
    private static final double VLF_MAX_FREQ = 0.04;
    private static final double LF_MIN_FREQ = 0.04;
    private static final double LF_MAX_FREQ = 0.15;
    private static final double HF_MIN_FREQ = 0.15;
    private static final double HF_MAX_FREQ = 0.4;

    // Function to calculate NN50
    public static int getNN50(ArrayList<Double> rrIntervals) {
        int nn50Count = 0;
        for (int i = 1; i < rrIntervals.size(); i++) {
            double diff = Math.abs(rrIntervals.get(i) - rrIntervals.get(i-1));
            if (diff > 0.05) { // 50 ms threshold
                nn50Count++;
            }
        }
        return nn50Count;
    }

    // Function to calculate SDNN
    public static double getSDNN(ArrayList<Double> rrIntervals) {
        double meanRR = getMeanRR(rrIntervals);
        double sumOfSquares = 0;
        for (double rr : rrIntervals) {
            sumOfSquares += Math.pow(rr - meanRR, 2);
        }
        double variance = sumOfSquares / (rrIntervals.size() - 1);
        return Math.sqrt(variance);
    }

    // Function to calculate HRV (RMSSD)
    public static double getHRV(ArrayList<Double> rrIntervals) {
        double sumOfSquares = 0;
        for (int i = 1; i < rrIntervals.size(); i++) {
            double diff = rrIntervals.get(i) - rrIntervals.get(i-1);
            sumOfSquares += Math.pow(diff, 2);
        }
        double variance = sumOfSquares / (rrIntervals.size() - 1);
        return Math.sqrt(variance);
    }

    // Function to calculate mean RR interval
    public static double getMeanRR(ArrayList<Double> rrIntervals) {
        double sum = 0;
        for (double rr : rrIntervals) {
            sum += rr;
        }
        return sum / rrIntervals.size();
    }
    // calculating the artifacts & removing redundant data..............
//    public static double calculateZscore(double[] data, double mean, double sd, int i) {
//        return (data[i] - mean) / sd;
//    }
//
//    // Function to detect and remove outliers based on Z-score threshold
//    public static double[] removeOutliers(double[] data, double threshold) {
//        double mean = Arrays.stream(data).average().getAsDouble(); // Calculate mean of data
//        double sd = Math.sqrt(Arrays.stream(data).map(x -> Math.pow(x - mean, 2)).average().getAsDouble()); // Calculate standard deviation of data
//        double[] zScores = new double[data.length];
//        for (int i = 0; i < data.length; i++) {
//            zScores[i] = calculateZscore(data, mean, sd, i); // Calculate Z-score for each data point
//        }
//        double[] cleanedData = Arrays.stream(data).filter(x -> Math.abs(calculateZscore(data, mean, sd, Arrays.asList(data).indexOf(x))) < threshold).toArray(); // Remove outliers based on Z-score threshold
//        return cleanedData;
//    }
}



