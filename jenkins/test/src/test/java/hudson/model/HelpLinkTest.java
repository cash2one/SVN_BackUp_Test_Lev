package hudson.model;

import static org.junit.Assert.assertTrue;

import com.gargoylesoftware.htmlunit.WebResponseListener;
import com.gargoylesoftware.htmlunit.html.DomNodeUtil;
import com.gargoylesoftware.htmlunit.html.HtmlElementUtil;
import hudson.tasks.BuildStepMonitor;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;

import java.util.List;

import hudson.tasks.Publisher;
import hudson.tasks.BuildStepDescriptor;
import hudson.model.HelpLinkTest.HelpNotFoundBuilder.DescriptorImpl;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Click all the help links and make sure they resolve to some text, not 404.
 *
 * @author Kohsuke Kawaguchi
 */
public class HelpLinkTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void systemConfig() throws Exception {
        clickAllHelpLinks(j.createWebClient().goTo("configure"));
    }

    @Test
    public void freestyleConfig() throws Exception {
        clickAllHelpLinks(j.createFreeStyleProject());
    }

    @Test
    public void mavenConfig() throws Exception {
        clickAllHelpLinks(j.createMavenProject());
    }

    @Test
    public void matrixConfig() throws Exception {
        clickAllHelpLinks(j.createMatrixProject());
    }

    private void clickAllHelpLinks(AbstractProject p) throws Exception {
        // TODO: how do we add all the builders and publishers so that we can test this meaningfully?
        clickAllHelpLinks(j.createWebClient(), p);
    }

    private void clickAllHelpLinks(JenkinsRule.WebClient webClient, AbstractProject p) throws Exception {
        // TODO: how do we add all the builders and publishers so that we can test this meaningfully?
        clickAllHelpLinks(webClient.getPage(p, "configure"));
    }

    private void clickAllHelpLinks(HtmlPage p) throws Exception {
        List<?> helpLinks = DomNodeUtil.selectNodes(p, "//a[@class='help-button']");
        assertTrue(helpLinks.size()>0);
        System.out.println("Clicking "+helpLinks.size()+" help links");

        for (HtmlAnchor helpLink : (List<HtmlAnchor>)helpLinks) {
            HtmlElementUtil.click(helpLink);
        }
    }

    public static class HelpNotFoundBuilder extends Publisher {
        public static final class DescriptorImpl extends BuildStepDescriptor {
            public boolean isApplicable(Class jobType) {
                return true;
            }

            @Override
            public String getHelpFile() {
                return "no-such-file/exists";
            }

            public String getDisplayName() {
                return "I don't have the help file";
            }
        }

        public BuildStepMonitor getRequiredMonitorService() {
            return BuildStepMonitor.BUILD;
        }
    }

    /**
     * Make sure that this test is meaningful.
     * Intentionally put 404 and verify that it's detected.
     */
    @Test
    public void negative() throws Exception {
        DescriptorImpl d = new DescriptorImpl();
        Publisher.all().add(d);
        try {
            FreeStyleProject p = j.createFreeStyleProject();
            p.getPublishersList().add(new HelpNotFoundBuilder());
            JenkinsRule.WebClient webclient = j.createWebClient();
            WebResponseListener.StatusListener statusListener = new WebResponseListener.StatusListener(404);
            webclient.addWebResponseListener(statusListener);

            clickAllHelpLinks(webclient, p);

            statusListener.assertHasResponses();
            String contentAsString = statusListener.getResponses().get(0).getContentAsString();
            Assert.assertTrue(contentAsString.contains(d.getHelpFile()));
        } finally {
            Publisher.all().remove(d);
        }
    }
}
