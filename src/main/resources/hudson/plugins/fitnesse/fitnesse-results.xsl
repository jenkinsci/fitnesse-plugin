<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="testResults">
	<hudson-fitnesse-plugin-report>
		<xsl:attribute name="plugin-version">0.2</xsl:attribute>
		<xsl:attribute name="fitnesse-version">
			<xsl:value-of select="FitNesseVersion"/>
		</xsl:attribute>

		<xsl:apply-templates select="//result"/>

		<summary>
			<xsl:attribute name="page" >
				<xsl:value-of select="rootPath"/>
			</xsl:attribute>
			<xsl:attribute name="duration">
				<xsl:value-of select="totalRunTimeInMillis"/>
			</xsl:attribute>
			<xsl:apply-templates select="finalCounts" />
		</summary>

	</hudson-fitnesse-plugin-report>
</xsl:template>

<xsl:template match="finalCounts">
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
		<xsl:attribute name="duration">
			<xsl:value-of select="runTimeInMillis"/>
		</xsl:attribute>
		<xsl:attribute name="content">
			<xsl:value-of select="content"/>
		</xsl:attribute>
	</detail>
</xsl:template>

</xsl:stylesheet>
