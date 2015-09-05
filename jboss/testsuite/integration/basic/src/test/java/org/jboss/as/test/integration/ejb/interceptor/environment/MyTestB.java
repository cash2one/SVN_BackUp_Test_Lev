package org.jboss.as.test.integration.ejb.interceptor.environment;

import javax.ejb.Stateless;

import org.jboss.logging.Logger;

@Stateless
public class MyTestB implements MyTestRemoteB {
    private static final Logger log = Logger.getLogger(MyTestB.class);

    MySession2RemoteB session23;

    public boolean doit() {
        log.info("Calling MyTest...");
        session23.doitSession();
        log.info("Calling MyTest - after doit");
        return true;
    }
}
