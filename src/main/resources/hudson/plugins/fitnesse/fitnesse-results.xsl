<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="testResults">
	<hudson-fitnesse-plugin-report>
		<xsl:attribute name="plugin-version">0.1</xsl:attribute>
		<xsl:attribute name="fitnesse-version">
			<xsl:value-of select="FitNesseVersion"/>
		</xsl:attribute>
		<xsl:attribute name="report-root">
			<xsl:value-of select="rootPath"/>
		</xsl:attribute>
		<xsl:apply-templates select="finalCounts">
			<xsl:with-param name="report-root">
				<xsl:value-of select="rootPath"/>
			</xsl:with-param>	
		</xsl:apply-templates>
		<xsl:apply-templates select="//result"/>	
	</hudson-fitnesse-plugin-report>
</xsl:template>

<xsl:template match="finalCounts">
	<xsl:param name="report-root"/>
	<summary>
		<xsl:attribute name="page">
			<xsl:value-of select="$report-root"/>
		</xsl:attribute>
		<xsl:attribute name="right">
			<xsl:value-of select="right"/>
		</xsl:attribute>
		<xsl:attribute name="wrong">
			<xsl:value-of select="wrong"/>
		</xsl:attribute>
		<xsl:attribute name="ignored">
			<xsl:value-of select="ignores"/>
		</xsl:attribute>
		<xsl:attribute name="exceptions">
			<xsl:value-of select="exceptions"/>
		</xsl:attribute>
	</summary>
</xsl:template>

<xsl:template match="result">
	<detail>
		<xsl:attribute name="page">
			<xsl:value-of select="substring-before(pageHistoryLink, '?')"/>
		</xsl:attribute>
		<xsl:attribute name="approxResultDate">
			<xsl:value-of select="substring-after(pageHistoryLink, 'resultDate=')"/>
		</xsl:attribute>
		<xsl:attribute name="name">
			<xsl:value-of select="relativePageName"/>
		</xsl:attribute>
		<xsl:attribute name="right">
			<xsl:value-of select="counts/right"/>
		</xsl:attribute>
		<xsl:attribute name="wrong">
			<xsl:value-of select="counts/wrong"/>
		</xsl:attribute>
		<xsl:attribute name="ignored">
			<xsl:value-of select="counts/ignores"/>
		</xsl:attribute>
		<xsl:attribute name="exceptions">
			<xsl:value-of select="counts/exceptions"/>
		</xsl:attribute>
	</detail>
</xsl:template>

</xsl:stylesheet>
