package hudson.plugins.fitnesse.project;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import hudson.model.Build;
import hudson.plugins.fitnesse.project.FitnesseProject;

public class FitnesseBuild extends Build<FitnesseProject,FitnesseBuild> {
    public FitnesseBuild(FitnesseProject project) throws IOException {
        super(project);
    }

    public FitnesseBuild(FitnesseProject project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    public FitnesseBuild(FitnesseProject job, Calendar timestamp) {
        super(job, timestamp);
    }


}
