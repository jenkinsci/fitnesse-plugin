package hudson.plugins.fitnesse;

import java.util.List;

import com.google.common.collect.Lists;
import hudson.model.AbstractProject;
import hudson.model.Action;
import org.kohsuke.stapler.StaplerProxy;

public class FitnesseHistoryAction implements StaplerProxy, Action {
    private AbstractProject<?, ?> project;

    public FitnesseHistoryAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    public Object getTarget() {
        List<String> pages = Lists.newArrayList("Foo", "Bar");

        return new FitnesseHistory(pages, null);
    }

    public String getIconFileName() {
        return "/plugin/fitnesse/icons/fitnesselogo-32x32.gif";
    }

    public String getDisplayName() {
        return "FitNesse History";
    }

    public String getUrlName() {
        return "fitnesseHistory";
    }

}
