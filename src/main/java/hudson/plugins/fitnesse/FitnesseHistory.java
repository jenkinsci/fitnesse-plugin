package hudson.plugins.fitnesse;

import java.util.List;

import hudson.model.AbstractProject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

public class FitnesseHistory {
    private AbstractProject<?, ?> owner;

    /** Pages, ordered by erraticness. */
    public final List<String> pages;

    /** History builds (containing details for each individual page in that build), in chronological order. */
    public final List<FitnesseResults> builds;

    public FitnesseHistory(AbstractProject<?, ?> owner, List<String> pages, List<FitnesseResults> builds) {
        this.pages = pages;
        this.owner = owner;
        this.builds = builds;
    }

    @Exported(visibility=2)
    public String getName() {
        return "FitNesse History";
    }

    public Object getDynamic(String token, StaplerRequest req,
                             StaplerResponse rsp) {
        return this;
    }

    public AbstractProject<?, ?> getOwner() {
        return owner;
    }
}