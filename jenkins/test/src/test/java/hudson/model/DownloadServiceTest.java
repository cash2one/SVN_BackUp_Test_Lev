package hudson.model;

import hudson.model.DownloadService.Downloadable;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import jenkins.model.DownloadSettings;
import net.sf.json.JSONObject;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.WithoutJenkins;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Kohsuke Kawaguchi
 */
public class DownloadServiceTest extends HudsonTestCase {
    private Downloadable job;

    /**
     * Makes sure that JavaScript on the client side for handling submission works.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (jenkins == null) {
            return;
        }
        // this object receives the submission.
        // to bypass the URL restriction, we'll trigger downloadService.download ourselves
        job = new Downloadable("test", "UNUSED");
        Downloadable.all().add(job);
        DownloadSettings.get().setUseBrowser(true);
    }

    @Issue("JENKINS-5536")
    public void testPost() throws Exception {
        // initially it should fail because the data doesn't have a signature
        assertNull(job.getData());
        createWebClient().goTo("/self/testPost");
        assertNull(job.getData());

        // and now it should work
        DownloadService.signatureCheck = false;
        try {
            createWebClient().goTo("/self/testPost");
            JSONObject d = job.getData();
            assertEquals(hashCode(),d.getInt("hello"));
        } finally {
            DownloadService.signatureCheck = true;
        }

        // TODO: test with a signature
    }

    /**
     * This is where the browser should hit to retrieve data.
     */
    public void doData(StaplerResponse rsp) throws IOException {
        rsp.setContentType("application/javascript");
        rsp.getWriter().println("downloadService.post('test',{'hello':"+hashCode()+"})");
    }

    @WithoutJenkins // could have been in core/src/test/ but update-center.json was already in test/src/test/ (used by UpdateSiteTest)
    public void testLoadJSON() throws Exception {
        assertRoots("[list]", "hudson.tasks.Maven.MavenInstaller.json"); // format used by most tools
        assertRoots("[data, version]", "hudson.tools.JDKInstaller.json"); // anomalous format
        assertRoots("[connectionCheckUrl, core, id, plugins, signature, updateCenterVersion]", "update-center.json");
    }

    private static void assertRoots(String expected, String file) throws Exception {
        URL resource = DownloadServiceTest.class.getResource(file);
        assertNotNull(file, resource);
        JSONObject json = JSONObject.fromObject(DownloadService.loadJSON(resource));
        @SuppressWarnings("unchecked") Set<String> keySet = json.keySet();
        assertEquals(expected, new TreeSet<String>(keySet).toString());
    }

}
