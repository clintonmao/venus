package com.saike.athena.reporter;

import com.meidusa.venus.extension.athena.AthenaProblemReporter;
import com.saic.framework.athena.site.helper.AthenaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by GodzillaHua on 7/3/16.
 */
public class DefaultProblemReporter implements AthenaProblemReporter {
    private static Logger logger = LoggerFactory.getLogger(DefaultProblemReporter.class);

    public void problem(String message, Throwable cause) {
        try{
            System.out.println("upload problem instance:" + this);
            AthenaUtils.getInstance().logError(message, cause);
        }catch (Exception e) {
            logger.error("report problem error", e);
        }
    }
}
