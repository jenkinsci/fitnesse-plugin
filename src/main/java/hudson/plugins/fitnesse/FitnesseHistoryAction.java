package hudson.plugins.fitnesse;

import java.util.List;

import com.google.common.collect.Lists;
import hudson.model.AbstractProject;
import hudson.model.Action;
import org.kohsuke.stapler.StaplerProxy;

public class FitnesseHistoryAction implements StaplerProxy, Action {
    private final FitnesseHistory history;

    public FitnesseHistoryAction(AbstractProject<?, ?> project) {
        List<String> pages = Lists.newArrayList("Foo", "Bar");
        this.history = new FitnesseHistory(project, pages, null);
    }

    public Object getTarget() {
        return history;
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
