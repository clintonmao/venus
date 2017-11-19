package com.meidusa.venus.monitor.athena.reporter;

/**
 * Created by GodzillaHua on 7/3/16.
 */
public interface MetricReporter {

    void metric(String key, int count);

}
