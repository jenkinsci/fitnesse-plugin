package hudson.plugins.fitnesse;

import hudson.plugins.fitnesse.NativePageCounts.Counts;

import java.util.List;

/**
 * A subclass of FitNesseResults that encapsulates multiple results into one
 * compound result.
 */
public class CompoundFitnesseResults extends FitnesseResults {

	private static final long serialVersionUID = 4703379870465222848L;

	public static FitnesseResults createFor(List<FitnesseResults> resultsList) {
		String page = "All Results";
		String resultsDate = null;
		int right = 0, wrong = 0, ignored = 0, exceptions = 0;
		
		for (FitnesseResults fitnesseResults : resultsList) {
			if (resultsDate == null) {
				resultsDate = fitnesseResults.getResultsDate();
			}
			right += fitnesseResults.getPassCount();
			wrong += fitnesseResults.getFailOnlyCount();
			ignored += fitnesseResults.getIgnoredCount();
			exceptions += fitnesseResults.getExceptionCount();
		}
		
		Counts counts = new Counts(page, resultsDate, right, wrong, ignored, exceptions, null);
		return new CompoundFitnesseResults(resultsList, counts);
	}
	
	public CompoundFitnesseResults(List<FitnesseResults> resultsList,
			Counts counts) {
		super(counts);
		for (FitnesseResults fitnesseResults : resultsList) {
			addChild(fitnesseResults);
		}
	}
}
