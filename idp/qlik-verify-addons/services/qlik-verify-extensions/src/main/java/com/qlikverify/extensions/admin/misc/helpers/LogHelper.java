package com.qlikverify.extensions.admin.misc.helpers;

import org.jboss.logging.Logger;

public class LogHelper {
    private static LogHelper LogHelper;
    private static final Logger logger = Logger.getLogger("QV::EXTENSIONS");
    public static LogHelper getInstance(){
        if(LogHelper == null) {
            LogHelper = new LogHelper();
        }
        return LogHelper;
    }

    public void info(String message){
        logger.info(message);
    }

    public void error(String message){
        logger.error(message);
    }

    public void debug(String message){
        logger.debug(message);
    }
}
