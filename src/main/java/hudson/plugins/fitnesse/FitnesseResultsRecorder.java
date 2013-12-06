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
import hudson.plugins.fitnesse.NativePageCounts.Counts;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
		return Collections
				.<Action> singleton(new FitnesseProjectAction(project));
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
			FilePath[] resultFiles = getResultFiles(build);
			FitnesseResults results = getResults(listener.getLogger(),
					resultFiles, build.getRootDir());
			if (results == null)
				return true; // no Fitnesse results found at all

			FitnesseResultsAction action = new FitnesseResultsAction(build,
					results);
			if (results.getBuildResult() != null)
				build.setResult(results.getBuildResult());
			build.addAction(action);
			return true;
		} catch (Throwable t) {
			t.printStackTrace(listener.getLogger());
			if (t instanceof InterruptedException)
				throw (InterruptedException) t;
			build.setResult(Result.FAILURE);
			return false;
		}
	}

	private FilePath[] getResultFiles(AbstractBuild<?, ?> build)
			throws IOException, InterruptedException {
		FilePath workingDirectory = FitnesseExecutor.getWorkingDirectory(build);
		return getResultFiles(workingDirectory);
	}

	public FilePath[] getResultFiles(FilePath workingDirectory)
			throws IOException, InterruptedException {
		FilePath resultsFile = FitnesseExecutor.getResultsFilePath(
				workingDirectory, fitnessePathToXmlResultsIn);

		if (resultsFile.exists()) {
			// directly configured single file
			return new FilePath[] { resultsFile };
		} else {
			// glob
			return workingDirectory.list(fitnessePathToXmlResultsIn);
		}
	}

	public FitnesseResults getResults(PrintStream logger,
			FilePath[] resultsFiles, File rootDir)
			throws IOException, TransformerException {
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

	public FitnesseResults getResults(PrintStream logger, FilePath resultsFile,
			File rootDir) throws IOException, TransformerException {
		InputStream resultsInputStream = null;
		try {
			logger.println("Reading results as "
					+ Charset.defaultCharset().displayName() + " from "
					+ resultsFile.getRemote());
			resultsInputStream = resultsFile.read();

			logger.println("Parsing results... ");
			NativePageCountsParser pageCountsParser = new NativePageCountsParser();
			NativePageCounts pageCounts = pageCountsParser
					.parse(resultsInputStream);

			logger.println("all-content: " + pageCounts.getAllContents().size());
			logger.println("resultsFile: " + getFitnessePathToXmlResultsIn());
			writeFitnesseResultFiles(logger, pageCounts, rootDir);

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
	 * See
	 * <tt>src/main/resources/hudson/plugins/fitnesse/FitnesseResultsRecorder/config.jelly</tt>
	 */
	@Extension
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher> {

		public FormValidation doCheckFitnessePathToXmlResultsIn(
				@QueryParameter String value) throws IOException,
				ServletException {
			if (value.length() == 0)
				return FormValidation
						.error("Please specify where to read fitnesse results from.");
			if (!value.endsWith("xml"))
				return FormValidation
						.warning("File does not end with 'xml': is that correct?");
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

	/**
	 * Gets a parsed fitnesse result and writes it to separate file. Putting the
	 * fitnesse result in a separate file as performance reasons. E.g. for a
	 * huge Test-Suite the actual fitnesse result can grow up to several MB.
	 * First implementation of fitnesse plugin has stored the result to the
	 * build.xml. This was very handy to present the result but slowed down
	 * jenkins since a request to the fitnesse result leat to putting the entire
	 * build.xml into the memory. With this function the fitnesse result is only
	 * load to memory if the user clicks on it.
	 * 
	 * @param logger
	 * @param pageCounts
	 * @param rootDir
	 */
	private void writeFitnesseResultFiles(PrintStream logger,
			NativePageCounts pageCounts, File rootDir) {
		String rootDirName = rootDir.getAbsolutePath() + "/";
		logger.println("write fitnesse results to: " + rootDirName);
		Map<String, String> allContent = pageCounts.getAllContents();
		logger.println("allContent:\n" + allContent.keySet());
		// iterate over all fitnesse tests in a suite
		for (Counts iCount : pageCounts.getAllCounts()) {
			String name = iCount.page;
			String content = allContent.get(name);
			if (null == content) {
				logger.println("could not find content for page: " + name);
				continue;
			}
			BufferedWriter out = null;
			String fileName = rootDirName + name;
			try {
				// Create separate file for every test in a suite
				FileWriter fstream = new FileWriter(fileName);
				out = new BufferedWriter(fstream);
				out.write(content);
				// Just store the path to the filename. 
				// Any help welcome how to
				// restore the path of the saved file when the user wants to see the details
				iCount.contentFile = fileName;
			} catch (IOException e) {
				logger.println("error while writing to out file" + fileName
						+ "\n" + e.toString());
			} finally {
				if (null != out) {
					try {
						out.close();
					} catch (IOException e) {
						logger.println("could not close out stream: " + "\n"
								+ fileName + e.toString());
					}
				}
			}
		}
	}
}
