package com.meidusa.venus.monitor.athena.reporter.impl;

import com.meidusa.venus.monitor.athena.reporter.ProblemReporter;
import com.saic.framework.athena.site.helper.AthenaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by GodzillaHua on 7/3/16.
 */
public class DefaultProblemReporter implements ProblemReporter {
    private static Logger logger = LoggerFactory.getLogger(DefaultProblemReporter.class);

    public void problem(String message, Throwable cause) {
        try{
            logger.info("upload problem instance:{}",this);
            AthenaUtils.getInstance().logError(message, cause);
        }catch (Exception e) {
            logger.error("report problem error", e);
        }
    }
}
