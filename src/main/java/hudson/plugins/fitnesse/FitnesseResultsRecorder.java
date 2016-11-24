package hudson.plugins.fitnesse;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.xml.transform.TransformerException;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class FitnesseResultsRecorder extends Recorder implements SimpleBuildStep {

	private final String fitnessePathToXmlResultsIn;

	@DataBoundConstructor
	public FitnesseResultsRecorder(String fitnessePathToXmlResultsIn) {
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
	public void perform(@Nonnull Run<?,?> build,@Nonnull FilePath workspace,@Nonnull Launcher launcher,@Nonnull TaskListener listener)
			throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();
		try {
			FilePath[] resultFiles = getResultFiles(logger, workspace);
			FitnesseResults results = getResults(logger, resultFiles, build.getRootDir());
			if (results == null)
				return; // no FitNesse results found at all

			FitnesseResultsAction action = new FitnesseResultsAction(build, results, listener);
			if (results.getBuildResult() != null)
				build.setResult(results.getBuildResult());
			build.addAction(action);
			return;
		} catch (InterruptedException e) { //aborted
			throw e;
		} catch (Throwable t) {
			t.printStackTrace(logger);
			build.setResult(Result.FAILURE);
			return;
		}
	}

	private FilePath[] getResultFiles(PrintStream logger, FilePath workspace) throws IOException,
			InterruptedException {
		return getResultFiles1(logger, workspace);
	}

	public FilePath[] getResultFiles1(PrintStream logger, FilePath workingDirectory) throws IOException,
			InterruptedException {
		FilePath resultsFile = FitnesseExecutor.getFilePath(logger, workingDirectory, fitnessePathToXmlResultsIn);

		if (resultsFile.exists()) {
			// directly configured single file
			return new FilePath[] { resultsFile };
		} else {
			// glob
			return workingDirectory.list(fitnessePathToXmlResultsIn);
		}
	}

	public FitnesseResults getResults(PrintStream logger, FilePath[] resultsFiles, File rootDir) throws IOException,
			TransformerException, InterruptedException {
		List<FitnesseResults> resultsList = new ArrayList<FitnesseResults>();

		for (FilePath filePath : resultsFiles) {
			FitnesseResults singleResults = getResults(logger, filePath, rootDir);
			resultsList.add(singleResults);
		}

		if (resultsList.isEmpty()) {
			return null;
		}
		if (resultsList.size() == 1) {
			return resultsList.get(0);
		}
		return CompoundFitnesseResults.createFor(resultsList);
	}

	public FitnesseResults getResults(PrintStream logger, FilePath resultsFile, File rootDir) throws IOException,
			TransformerException, InterruptedException {
		InputStream resultsInputStream = null;
		try {
			logger.println("Reading results as " + Charset.defaultCharset().displayName() + " from "
					+ resultsFile.getRemote());
			resultsInputStream = resultsFile.read();

			Path p = Paths.get(resultsFile.getRemote());
			String resultFileName = p.getFileName().toString();

			logger.println("Parsing results... ");
			NativePageCountsParser pageCountsParser = new NativePageCountsParser();
			NativePageCounts pageCounts = pageCountsParser.parse(resultsInputStream, resultFileName, logger, rootDir.getAbsolutePath()
					+ System.getProperty("file.separator"));
			logger.println("resultsFile: " + getFitnessePathToXmlResultsIn());

			logger.println("Got results: " + pageCounts.getSummary());
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
	 * See <tt>src/main/resources/hudson/plugins/fitnesse/FitnesseResultsRecorder/config.jelly</tt>
	 */
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public FormValidation doCheckFitnessePathToXmlResultsIn(@QueryParameter String value) throws IOException,
				ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please specify where to read FitNesse results from.");
			if (!value.endsWith("xml"))
				return FormValidation.warning("File does not end with 'xml': is that correct?");
			return FormValidation.ok();
		}

		/**
		 * {@link BuildStepDescriptor}
		 */
		@Override
		@SuppressWarnings("rawtypes")
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
