package hudson.plugins.fitnesse;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.ModelObject;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
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
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.xml.transform.TransformerException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class FitnesseResultsRecorder extends Recorder {

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
	 * {@link Recorder}
	 */
	@Override
	public Collection<Action> getProjectActions(AbstractProject<?, ?> project) {
		final Collection<Action> list = new ArrayList<Action>();
		list.add(new FitnesseProjectAction(project));
		list.add(new FitnesseHistoryAction(project));
		return list;
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
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();
		try {
			FilePath[] resultFiles = getResultFiles(logger, build);
			FitnesseResults results = getResults(logger, resultFiles, build.getRootDir());
			if (results == null)
				return true; // no Fitnesse results found at all

			FitnesseResultsAction action = new FitnesseResultsAction(build, results);
			if (results.getBuildResult() != null)
				build.setResult(results.getBuildResult());
			build.addAction(action);
			return true;
		} catch (InterruptedException e) { //aborted
			throw e;
		} catch (Throwable t) {
			t.printStackTrace(logger);
			build.setResult(Result.FAILURE);
			return false;
		}
	}

	private FilePath[] getResultFiles(PrintStream logger, AbstractBuild<?, ?> build) throws IOException,
			InterruptedException {
		FilePath workingDirectory = FitnesseExecutor.getWorkingDirectory(logger, build);
		return getResultFiles(logger, workingDirectory);
	}

	public FilePath[] getResultFiles(PrintStream logger, FilePath workingDirectory) throws IOException,
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
		return DESCRIPTOR;
	}

	private static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	/**
	 * See
	 * <tt>src/main/resources/hudson/plugins/fitnesse/FitnesseResultsRecorder/config.jelly</tt>
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
