package hudson.plugins.fitnesse;

import hudson.FilePath;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.*;
import java.io.*;

/**
 * Created by surat_das on 4/27/2017.
 */
public class ConvertReport {

    public static void generateJunitResult(FilePath inputFilePath, FilePath outputFilePath) throws InterruptedException,IOException,TransformerException {

        Reader reader = new StringReader(getFitnesseToJunitResultStyle());
        Source stylesheetSource = new StreamSource(reader);

        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        Transformer transformer = factory.newTransformer(stylesheetSource);

        Source inputSource = new StreamSource(inputFilePath.read());
        OutputStream junitResultsStream = outputFilePath.write();
        Result outputResult = new StreamResult(junitResultsStream);

        transformer.transform(inputSource, outputResult);
        junitResultsStream.close();
    }


    private static String getFitnesseToJunitResultStyle() {
        return
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                        "<xsl:template match=\"/\">\n" +
                        "  <xsl:element name=\"testsuite\">\n" +
                        "    <xsl:attribute name=\"tests\">\n" +
                        "      <xsl:value-of select=\"sum(testResults/finalCounts/*)\" />\n" +
                        "    </xsl:attribute>\n" +
                        "    <xsl:attribute name=\"failures\">\n" +
                        "      <xsl:value-of select=\"testResults/finalCounts/wrong\" />\n" +
                        "    </xsl:attribute>\n" +
                        "    <xsl:attribute name=\"disabled\">\n" +
                        "      <xsl:value-of select=\"testResults/finalCounts/ignores\" />\n" +
                        "    </xsl:attribute>\n" +
                        "    <xsl:attribute name=\"errors\">\n" +
                        "      <xsl:value-of select=\"testResults/finalCounts/exceptions\" />\n" +
                        "    </xsl:attribute>\n" +
                        "    <xsl:attribute name=\"time\">\n" +
                        "      <xsl:value-of select=\"testResults/totalRunTimeInMillis div 1000\" />\n" +
                        "    </xsl:attribute>\n" +
                        "    <xsl:attribute name=\"name\">AcceptanceTests</xsl:attribute>\n" +
                        "  <xsl:for-each select=\"testResults/result\">\n" +
                        "    <xsl:element name=\"testcase\">\n" +
                        "      <xsl:attribute name=\"classname\">\n" +
                        "        <xsl:value-of select=\"/testResults/rootPath\" />\n" +
                        "      </xsl:attribute>\n" +
                        "      <xsl:attribute name=\"name\">\n" +
                        "        <xsl:value-of select=\"relativePageName\" />\n" +
                        "      </xsl:attribute>\n" +
                        "      <xsl:attribute name=\"time\">\n" +
                        "        <xsl:value-of select=\"runTimeInMillis div 1000\" />\n" +
                        "      </xsl:attribute>\n" +
                        "      <xsl:choose>\n" +
                        "        <xsl:when test=\"counts/exceptions > 0\">\n" +
                        "          <xsl:element name=\"error\">\n" +
                        "            <xsl:attribute name=\"message\">\n" +
                        "              <xsl:value-of select=\"counts/exceptions\" />\n" +
                        "              <xsl:text> exceptions thrown</xsl:text>\n" +
                        "              <xsl:if test=\"counts/wrong > 0\">\n" +
                        "                <xsl:text> and </xsl:text>\n" +
                        "                <xsl:value-of select=\"counts/wrong\" />\n" +
                        "                <xsl:text> assertions failed</xsl:text>\n" +
                        "              </xsl:if>\n" +
                        "            </xsl:attribute>\n" +
                        "          </xsl:element>\n" +
                        "        </xsl:when>\n" +
                        "        <xsl:when test=\"counts/wrong > 0\">\n" +
                        "          <xsl:element name=\"failure\">\n" +
                        "            <xsl:attribute name=\"message\">\n" +
                        "              <xsl:value-of select=\"counts/wrong\" />\n" +
                        "              <xsl:text> assertions failed</xsl:text>\n" +
                        "            </xsl:attribute>\n" +
                        "          </xsl:element>\n" +
                        "        </xsl:when>\n" +
                        "      </xsl:choose>\n" +
                        "    </xsl:element>\n" +
                        "  </xsl:for-each>\n" +
                        "  </xsl:element>\n" +
                        "</xsl:template>\n" +
                        "</xsl:stylesheet>";
    }
}
