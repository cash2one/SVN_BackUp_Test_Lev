/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.visualizers.backend;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.util.JMeterUtils;

/**
 * Sampler metric
 * @since 2.13
 */
public class SamplerMetric {
    private static final int SLIDING_WINDOW_SIZE = JMeterUtils.getPropDefault("backend_metrics_window", 100); //$NON-NLS-1$
    
    // Response times for OK samples
    // Limit to sliding window of SLIDING_WINDOW_SIZE values 
    private DescriptiveStatistics okResponsesStats = new DescriptiveStatistics(SLIDING_WINDOW_SIZE);
    // Response times for KO samples
    // Limit to sliding window of SLIDING_WINDOW_SIZE values 
    private DescriptiveStatistics koResponsesStats = new DescriptiveStatistics(SLIDING_WINDOW_SIZE);
    // Response times for All samples
    // Limit to sliding window of SLIDING_WINDOW_SIZE values 
    private DescriptiveStatistics allResponsesStats = new DescriptiveStatistics(SLIDING_WINDOW_SIZE);
    private int successes;
    private int failures;
    private int hits;
    /**
     * 
     */
    public SamplerMetric() {
    }

    /**
     * Add a {@link SampleResult} to be used in the statistics
     * @param result {@link SampleResult} to be used
     */
    public synchronized void add(SampleResult result) {
        if(result.isSuccessful()) {
            successes+=result.getSampleCount()-result.getErrorCount();
        } else {
            failures+=result.getErrorCount();
        }
        long time = result.getTime();
        allResponsesStats.addValue(time);
        if(result.isSuccessful()) {
            // Should we also compute KO , all response time ?
            // only take successful requests for time computing
            okResponsesStats.addValue(time);
        }else {
            koResponsesStats.addValue(time);
        }
        addHits(result);
    }

    /**
     * Compute hits from res
     * @param res {@link SampleResult}
     */
    private void addHits(SampleResult res) {     
        SampleResult[] subResults = res.getSubResults();
        if (!TransactionController.isFromTransactionController(res)) {
            hits += 1;                 
        }
        for (int i = 0; i < subResults.length; i++) {            
            addHits(subResults[i]);
        }
    }
    
    /**
     * Reset metric except for percentile related data
     */
    public synchronized void resetForTimeInterval() {
        // We don't clear responsesStats nor usersStats as it will slide as per my understanding of 
        // http://commons.apache.org/proper/commons-math/userguide/stat.html
        successes = 0;
        failures = 0;
        hits = 0;
    }

    /**
     * Get the number of total requests for the current time slot
     * 
     * @return number of total requests
     */
    public int getTotal() {
        return successes+failures;
    }
    
    /**
     * Get the number of successful requests for the current time slot
     * 
     * @return number of successful requests
     */
    public int getSuccesses() {
        return successes;
    }

    /**
     * Get the number of failed requests for the current time slot
     * 
     * @return number of failed requests
     */
    public int getFailures() {
        return failures;
    }

    /**
     * Get the maximal elapsed time for requests within sliding window
     * 
     * @return the maximal elapsed time, or <code>0</code> if no requests have
     *         been added yet
     */
    public double getOkMaxTime() {
        return okResponsesStats.getMax();
    }

    /**
     * Get the minimal elapsed time for requests within sliding window
     * 
     * @return the minTime, or {@link Long#MAX_VALUE} if no requests have been
     *         added yet
     */
    public double getOkMinTime() {
        return okResponsesStats.getMin();
    }
    
    /**
     * Get the arithmetic mean of the stored values
     * 
     * @return The arithmetic mean of the stored values
     */
    public double getOkMean() {
        return okResponsesStats.getMean();
    }
    
    /**
     * Returns an estimate for the requested percentile of the stored values.
     * 
     * @param percentile
     *            the requested percentile (scaled from 0 - 100)
     * @return Returns an estimate for the requested percentile of the stored
     *         values.
     */
    public double getOkPercentile(double percentile) {
        return okResponsesStats.getPercentile(percentile);
    }

    /**
     * Get the maximal elapsed time for requests within sliding window
     * 
     * @return the maximal elapsed time, or <code>0</code> if no requests have
     *         been added yet
     */
    public double getKoMaxTime() {
        return koResponsesStats.getMax();
    }

    /**
     * Get the minimal elapsed time for requests within sliding window
     * 
     * @return the minTime, or {@link Long#MAX_VALUE} if no requests have been
     *         added yet
     */
    public double getKoMinTime() {
        return koResponsesStats.getMin();
    }
    
    /**
     * Get the arithmetic mean of the stored values
     * 
     * @return The arithmetic mean of the stored values
     */
    public double getKoMean() {
        return koResponsesStats.getMean();
    }
    
    /**
     * Returns an estimate for the requested percentile of the stored values.
     * 
     * @param percentile
     *            the requested percentile (scaled from 0 - 100)
     * @return Returns an estimate for the requested percentile of the stored
     *         values.
     */
    public double getKoPercentile(double percentile) {
        return koResponsesStats.getPercentile(percentile);
    }
    
    /**
     * Get the maximal elapsed time for requests within sliding window
     * 
     * @return the maximal elapsed time, or <code>0</code> if no requests have
     *         been added yet
     */
    public double getAllMaxTime() {
        return allResponsesStats.getMax();
    }

    /**
     * Get the minimal elapsed time for requests within sliding window
     * 
     * @return the minTime, or {@link Long#MAX_VALUE} if no requests have been
     *         added yet
     */
    public double getAllMinTime() {
        return allResponsesStats.getMin();
    }
    
    /**
     * Get the arithmetic mean of the stored values
     * 
     * @return The arithmetic mean of the stored values
     */
    public double getAllMean() {
        return allResponsesStats.getMean();
    }
    
    /**
     * Returns an estimate for the requested percentile of the stored values.
     * 
     * @param percentile
     *            the requested percentile (scaled from 0 - 100)
     * @return Returns an estimate for the requested percentile of the stored
     *         values.
     */
    public double getAllPercentile(double percentile) {
        return allResponsesStats.getPercentile(percentile);
    }

    /**
     * Returns hits to server
     * @return the hits
     */
    public int getHits() {
        return hits;
    }
}
