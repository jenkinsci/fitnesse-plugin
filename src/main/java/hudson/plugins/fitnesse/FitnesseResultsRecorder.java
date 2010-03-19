package hudson.plugins.fitnesse;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.ModelObject;
import hudson.model.Result;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.xml.transform.TransformerException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class FitnesseResultsRecorder extends Recorder {
	
	private final String fitnessePathToXmlResultsIn;

	@DataBoundConstructor
	public FitnesseResultsRecorder(String fitnessePathToXmlResultsIn) throws TransformerException {
		this.fitnessePathToXmlResultsIn = fitnessePathToXmlResultsIn;
	}

	/**
	 * referenced in config.jelly
	 */
	public String getFitnessePathToXmlResultsIn() {
		return fitnessePathToXmlResultsIn;
	}

	/**
	 * {@link BuildStep}
	 */
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	/**
	 * {@link BuildStep}
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		try {
			File resultsFile = new File(fitnessePathToXmlResultsIn);
			FitnesseResults results = getResults(listener, resultsFile);
			FitnesseResultsAction action = new FitnesseResultsAction(build, results);
			
			if (results.getBuildResult() != null) build.setResult(results.getBuildResult());
			build.addAction(action);
			return true;
		} catch (Throwable t) {
			t.printStackTrace(listener.getLogger());
			if (t instanceof InterruptedException) throw (InterruptedException) t;
			build.setResult(Result.FAILURE);
			return false;
		}
	}

	private FitnesseResults getResults(BuildListener listener, File resultsFile) throws IOException, TransformerException {
		InputStream resultsInputStream = null;
		try {
			listener.getLogger().println("Reading results as " + Charset.defaultCharset().displayName() 
					+ " from " + resultsFile.getAbsolutePath());
			resultsInputStream = new BufferedInputStream(new FileInputStream(resultsFile));
			
			listener.getLogger().println("Parsing results... ");
			NativePageCountsParser pageCountsParser = new NativePageCountsParser();
			NativePageCounts pageCounts = pageCountsParser.parse(resultsInputStream);
			
			listener.getLogger().println("Got results: " + pageCounts.getSummary());
			return new FitnesseResults(pageCounts);
		} finally {
			if (resultsInputStream != null) {
				try {
					resultsInputStream.close();
				} catch (Exception e) {
					// swallow
				}
			}
		}
	}

	/**
	 * {@link Publisher}
	 */
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
     *  See <tt>src/main/resources/hudson/plugins/fitnesse/FitnesseResultsRecorder/config.jelly</tt>
     */
    @Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		
		public FormValidation doCheckFitnessePathToXmlResultsIn(@QueryParameter String value) throws IOException, ServletException {
        	if (value.length()==0)
        		return FormValidation.error("Please specify where to read fitnesse results from.");
        	File file = new File(value);
			if (! file.exists()) {
        		if (!(file.getParent() != null && file.getParentFile().exists()))
        			return FormValidation.warning("Path does not exist.");
			}
        	if (!value.endsWith("xml"))
        		return FormValidation.warning("Location does not end with 'xml': is that correct?");
        	return FormValidation.ok();
		}
        
		/**
         * {@link BuildStepDescriptor}
         */
        @Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			// works with any kind of project
			return true;
		}

		/**
         * {@link ModelObject} 
         */
		@Override
		public String getDisplayName() {
			return "Publish Fitnesse results report";
		}
		
	}

}
