package hudson.plugins.fitnesse;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Execute fitnesse tests, either by starting a new fitnesse instance or by
 * using a fitnesse instance running elsewhere.
 *
 * @author Tim Bacon
 */
public class FitnesseBuilder extends Builder implements SimpleBuildStep{

	public static final String START_FITNESSE = "fitnesseStart";
	public static final String FITNESSE_HOST = "fitnesseHost";
	public static final String FITNESSE_PORT = "fitnessePort";
	public static final String FITNESSE_PORT_REMOTE = "fitnessePortRemote";
    public static final String FITNESSE_ENABLE_SSL = "fitnesseEnableSsl";
	public static final String FITNESSE_PORT_LOCAL = "fitnessePortLocal";
	public static final String FITNESSE_ADDITIONAL_OPTIONS = "additionalFitnesseOptions";
	public static final String JAVA_OPTS = "fitnesseJavaOpts";
	public static final String FITNESSE_JDK = "fitnesseJdk";
	public static final String PATH_TO_JAR = "fitnessePathToJar";
	public static final String PATH_TO_ROOT = "fitnessePathToRoot";
	public static final String TARGET_PAGE = "fitnesseTargetPage";
	public static final String TARGET_IS_SUITE = "fitnesseTargetIsSuite";
	public static final String PATH_TO_RESULTS = "fitnessePathToXmlResultsOut";
	public static final String HTTP_TIMEOUT = "fitnesseHttpTimeout";
	public static final String TEST_TIMEOUT = "fitnesseTestTimeout";
	public static final String JAVA_WORKING_DIRECTORY = "fitnesseJavaWorkingDirectory";

	static final int _URL_READ_TIMEOUT_MILLIS = 60 * 1000;
	static final String _LOCALHOST = "localhost";
	static final String _HOSTNAME_SLAVE_PROPERTY = "HOST_NAME";

	private Map<String, String> options;

	@DataBoundConstructor
	public FitnesseBuilder(Map<String, String> options) {
		// Use n,v pairs to ease future extension
		this.options = options;
	}

	private String getOption(String key, String valueIfKeyNotFound) {
		if (options.containsKey(key)) {
			String value = options.get(key);
			if (value != null && !"".equals(value))
				return value;
		}
		return valueIfKeyNotFound;
	}

	private String getOption(String key, String valueIfKeyNotFound, EnvVars environment) {
		if (environment != null) {
			if (options.containsKey(key)) {
				String value = options.get(key);
				if (value != null && !"".equals(value))
					return Util.replaceMacro(value, environment);
			} else
				return valueIfKeyNotFound;
		} else
			return getOption(key, valueIfKeyNotFound);

		return valueIfKeyNotFound;
	}

	/**
	 * referenced in config.jelly
	 */
	public boolean getFitnesseStart() {
		return Boolean.parseBoolean(getOption(START_FITNESSE, "False"));
	}

    /**
     * referenced in config.jelly
     */
    public boolean getFitnesseSsl() {
        return Boolean.parseBoolean(getOption(FITNESSE_ENABLE_SSL, "False"));
    }

	/**
	 * referenced in config.jelly
	 */
	public String getFitnesseHost() {
		if (getFitnesseStart()) {
			return _LOCALHOST;
		} else {
			return getOption(FITNESSE_HOST, "unknown_host");
		}
	}

	public String getFitnesseHost(EnvVars environment) {
		if (getFitnesseStart()) {
			return _LOCALHOST;
		} else {
			return getOption(FITNESSE_HOST, "unknown_host", environment);
		}
	}

	public String getFitnesseHost(Run<?,?> build, EnvVars environment) throws IOException, InterruptedException {
		if (getFitnesseStart()) {
			if (build != null && environment != null && environment.get(_HOSTNAME_SLAVE_PROPERTY) != null) {
				return environment.get(_HOSTNAME_SLAVE_PROPERTY);
			} else {
				return _LOCALHOST;
			}
		} else
			return getOption(FITNESSE_HOST, "unknown_host", environment);
	}

	/**
	 * referenced in config.jelly
	 */
	public String getFitnesseJdk() {
		return getOption(FITNESSE_JDK, "");
	}

	public String getFitnesseJdk(EnvVars environment) {
		return getOption(FITNESSE_JDK, "", environment);
	}

	/**
	 * referenced in config.jelly
	 */
	public String getFitnesseJavaOpts() {
		return getOption(JAVA_OPTS, "");
	}

	public String getFitnesseJavaOpts(EnvVars environment) {
		return getOption(JAVA_OPTS, "", environment);
	}

	/**
	 * referenced in config.jelly
	 */
	public String getFitnesseJavaWorkingDirectory() {
		String fitnessePathToJar = getFitnessePathToJar(), fitnesseJarDir = "";
		if (!"".equals(fitnessePathToJar)) {
			File jarFile = new File(fitnessePathToJar);
			if (jarFile.exists()) {
				fitnesseJarDir = jarFile.getParentFile().getAbsolutePath();
			} else {
				fitnesseJarDir = jarFile.getParent();
				if (fitnesseJarDir == null)
					fitnesseJarDir = "";
			}
		}
		return getOption(JAVA_WORKING_DIRECTORY, fitnesseJarDir);
	}

	/**
	 * referenced in config.jelly
	 */
	public String getFitnessePort() {
		return getOption(FITNESSE_PORT_REMOTE, getOption(FITNESSE_PORT_LOCAL, getOption(FITNESSE_PORT, "-1")));
	}

	public int getFitnessePort(EnvVars environment) {
		return Integer.parseInt(getOption(FITNESSE_PORT_REMOTE,
				getOption(FITNESSE_PORT_LOCAL, getOption(FITNESSE_PORT, "-1", environment), environment), environment));
	}

	/**
	 * referenced in config.jelly
	 */
	public String getFitnessePathToJar() {
		return getOption(PATH_TO_JAR, "fitnesse.jar");
	}

	/**
	 * referenced in config.jelly Defaults to empty string
	 */
	public String getAdditionalFitnesseOptions() {
		String sanitizedOptions = getOption(FITNESSE_ADDITIONAL_OPTIONS, "");
		// remove quotes that Jenkins config wraps around anything with a space in
		// it
		if (sanitizedOptions.length() > 2 && sanitizedOptions.startsWith("\"") && sanitizedOptions.endsWith("\"")) {
			sanitizedOptions = sanitizedOptions.substring(1, sanitizedOptions.length() - 1);
		}
		return sanitizedOptions;
	}

	/**
	 * referenced in config.jelly
	 */
	public String getFitnessePathToRoot() {
		return getOption(PATH_TO_ROOT, "FitNesseRoot");
	}

	public String getFitnessePathToRoot(EnvVars environment) {
		return getOption(PATH_TO_ROOT, "FitNesseRoot", environment);
	}

	/**
	 * referenced in config.jelly
	 */
	public String getFitnesseTargetPage() {
		return getOption(TARGET_PAGE, "");
	}

	public String getFitnesseTargetPage(EnvVars environment) {
		return getOption(TARGET_PAGE, "", environment);
	}

	/**
	 * referenced in config.jelly
	 */
	public boolean getFitnesseTargetIsSuite() {
		return Boolean.parseBoolean(getOption(TARGET_IS_SUITE, "False"));
	}

	/**
	 * referenced in config.jelly
	 */
	public String getFitnessePathToXmlResultsOut() {
		return getOption(PATH_TO_RESULTS, "fitnesse-results.xml");
	}

	public String getFitnessePathToXmlResultsOut(EnvVars environment) {
		return getOption(PATH_TO_RESULTS, "fitnesse-results.xml", environment);
	}

	/**
	 * referenced in config.jelly
	 */
	public String getFitnesseHttpTimeout() {
		return getOption(HTTP_TIMEOUT, String.valueOf(_URL_READ_TIMEOUT_MILLIS));
	}

	public int getFitnesseHttpTimeout(EnvVars environment) {
		return Integer.parseInt(getOption(HTTP_TIMEOUT, String.valueOf(_URL_READ_TIMEOUT_MILLIS), environment));
	}

	/**
	 * referenced in config.jelly
	 */
	public int getFitnesseTestTimeout() {
		return Integer.parseInt(getOption(TEST_TIMEOUT, String.valueOf(_URL_READ_TIMEOUT_MILLIS)));
	}

	public int getFitnesseTestTimeout(EnvVars environment) {
		return Integer.parseInt(getOption(TEST_TIMEOUT, String.valueOf(_URL_READ_TIMEOUT_MILLIS), environment));
	}

	/**
	 * {@link Builder}
	 */
	@Override
	public void perform(@Nonnull Run<?,?> build,@Nonnull FilePath workspace,@Nonnull Launcher launcher,@Nonnull TaskListener listener) throws IOException,
			InterruptedException {
		listener.getLogger().println(getClass().getName() + ": " + options);
		FitnesseExecutor fitnesseExecutor = new FitnesseExecutor(this, listener, build.getEnvironment(listener));
		fitnesseExecutor.execute(launcher, workspace, build);
	}

	/**
	 * See <tt>src/main/resources/hudson/plugins/fitnesse/FitnesseBuilder/config.jelly</tt>
	 */
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		public FormValidation doCheckFitnesseHost(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please specify the host of the fitnesse instance.");
			return FormValidation.ok();
		}

		public FormValidation doCheckAdditionalFitnesseOptions(@QueryParameter String value) throws IOException,
				ServletException {
			if (value.length() > 0) {
				if (value.contains("-r") || value.contains("-p") || value.contains("-d"))
					return FormValidation.error("Please use the appropriate config fields to specify options for -r, -d, and -p.");
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckFitnessePort(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please specify the fitnesse port.");
			try {
				int intValue = Integer.parseInt(value);
				if (intValue < 1)
					return FormValidation.error("Port must be a positive integer.");
			} catch (NumberFormatException e) {
				if (!value.startsWith("$"))
					return FormValidation.error("Port must be a number.");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckFitnesseJdk(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.ok("Defaults to project's JDK");
			return FormValidation.ok();
		}

		public FormValidation doCheckFitnesseJavaOpts(@QueryParameter String value) throws IOException, ServletException {
			return FormValidation.ok();
		}

		public FormValidation doCheckFitnesseJavaWorkingDirectory(@QueryParameter String value) throws IOException,
				ServletException {
			if (value.length() == 0)
				return FormValidation.ok("Location of fitnesse.jar will be used as java working directory.");
			if (!new File(value).exists())
				return FormValidation.error("Path does not exist.");
			return FormValidation.ok();
		}

		public FormValidation doCheckFitnessePathToJar(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please specify the path to 'fitnesse.jar'.");
			if (!value.endsWith("fitnesse.jar") && new File(value, "fitnesse.jar").exists())
				return FormValidation.warning("Path does not end with 'fitnesse.jar': is that correct?");
			return FormValidation.ok();
		}

		public FormValidation doCheckFitnessePathToRoot(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please specify the location of 'FitNesseRoot'.");
			if (!value.endsWith("FitNesseRoot") && new File(value, "FitNesseRoot").exists())
				return FormValidation.warning("Path does not end with 'FitNesseRoot': is that correct?");
			return FormValidation.ok();
		}

		public FormValidation doCheckFitnesseTargetPage(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please specify a page to execute.");
			return FormValidation.ok();
		}

		public FormValidation doCheckFitnesseTargetIsSuite(@QueryParameter String value) throws IOException,
				ServletException {
			return FormValidation.ok();
		}

		public FormValidation doCheckFitnesseHttpTimeout(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.ok("Default HTTP timeout " + _URL_READ_TIMEOUT_MILLIS + "ms will be used.");
			try {
				if (Integer.parseInt(value) < 0)
					return FormValidation.error("HTTP timeout must be a positive integer.");
			} catch (NumberFormatException e) {
				if (!value.startsWith("$"))
					return FormValidation.error("HTTP timeout must be a number.");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckFitnesseTestTimeout(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.ok("Default test timeout " + _URL_READ_TIMEOUT_MILLIS + "ms will be used.");
			try {
				if (Integer.parseInt(value) < 0)
					return FormValidation.error("Test timeout must be a positive integer.");
			} catch (NumberFormatException e) {
				if (!value.startsWith("$"))
					return FormValidation.error("Test timeout must be a number.");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckFitnessePathToXmlResultsOut(@QueryParameter String value) throws IOException,
				ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please specify where to write fitnesse results to.");
			if (!value.endsWith("xml"))
				return FormValidation.warning("File does not end with 'xml': is that correct?");
			return FormValidation.ok();
		}

		/**
		 * {@link BuildStepDescriptor}
		 */
		@Override
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// indicates that this builder can be used with all kinds of project types
			return true;
		}

		/**
		 * {@link ModelObject}
		 */
		@Override
		public String getDisplayName() {
			return "Execute FitNesse tests";
		}

		/**
		 * {@link Descriptor} config.jelly uses hide-able fields so take control of
		 * instance creation
		 */
		@Override
		public FitnesseBuilder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			String startFitnesseValue = formData.getJSONObject(START_FITNESSE).getString("value");
			if (Boolean.parseBoolean(startFitnesseValue)) {
				return newFitnesseBuilder(
						startFitnesseValue,
						collectFormData(formData, new String[] { FITNESSE_JDK, JAVA_OPTS, JAVA_WORKING_DIRECTORY, PATH_TO_JAR,
								PATH_TO_ROOT, FITNESSE_PORT_LOCAL, TARGET_PAGE, TARGET_IS_SUITE, HTTP_TIMEOUT, TEST_TIMEOUT,
								PATH_TO_RESULTS, FITNESSE_ADDITIONAL_OPTIONS }));
			}
			return newFitnesseBuilder(
					startFitnesseValue,
					collectFormData(formData, new String[] { FITNESSE_HOST, FITNESSE_PORT_REMOTE, FITNESSE_ENABLE_SSL, TARGET_PAGE, TARGET_IS_SUITE,
							HTTP_TIMEOUT, TEST_TIMEOUT, PATH_TO_RESULTS }));
		}

		private FitnesseBuilder newFitnesseBuilder(String startFitnesseValue, Map<String, String> collectedFormData) {
			collectedFormData.put(START_FITNESSE, startFitnesseValue);
			return new FitnesseBuilder(collectedFormData);
		}

		private Map<String, String> collectFormData(JSONObject formData, String[] keys) {
			Map<String, String> targetElements = new HashMap<String, String>();
			for (String key : keys) {
				String value = "";
				if (formData.has(key)) {
					value = formData.getString(key);
				} else if (formData.getJSONObject(START_FITNESSE).get(key) != null) {
					value = formData.getJSONObject(START_FITNESSE).getString(key);
				}
				targetElements.put(key, value);
			}
			return targetElements;
		}

	}
}
