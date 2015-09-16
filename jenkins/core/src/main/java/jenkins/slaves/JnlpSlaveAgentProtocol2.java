package jenkins.slaves;

import hudson.Extension;
import org.jenkinsci.remoting.nio.NioChannelHub;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

/**
 * {@link JnlpSlaveAgentProtocol} Version 2.
 *
 * <p>
 * This protocol extends the version 1 protocol by adding a per-client cookie,
 * so that we can detect a reconnection from the slave and take appropriate action,
 * when the connection disappeared without the master noticing.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.467
 */
@Extension
public class JnlpSlaveAgentProtocol2 extends JnlpSlaveAgentProtocol {
    @Override
    public String getName() {
        return "JNLP2-connect";
    }

    @Override
    public void handle(Socket socket) throws IOException, InterruptedException {
        new Handler2(hub.getHub(),socket).run();
    }

    protected static class Handler2 extends Handler {
        /**
         * @deprecated as of 1.559
         *      Use {@link #Handler2(NioChannelHub, Socket)}
         */
        @Deprecated
        public Handler2(Socket socket) throws IOException {
            super(socket);
        }

        public Handler2(NioChannelHub hub, Socket socket) throws IOException {
            super(hub, socket);
        }

        /**
         * Handles JNLP slave agent connection request (v2 protocol)
         */
        @Override
        protected void run() throws IOException, InterruptedException {
            request.load(new ByteArrayInputStream(in.readUTF().getBytes("UTF-8")));

            final String nodeName = request.getProperty("Node-Name");

            for (JnlpAgentReceiver recv : JnlpAgentReceiver.all()) {
                if (recv.handle(nodeName,this))
                    return;
            }

            error("Unrecognized name: "+nodeName);
        }
    }
}
