<?xml version='1.0'?>
<!--
		DO NOT EDIT THIS FILE UNLESS YOU ARE IN THE DOCUMENTATION PROJECT

		This file is shared by all ProActive projects. If you have to modify it,
		please refer to the INSTALL file in the root of the Documentation project
		to know how to do it properly.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">		

	<xsl:param name="tabReplacement" select="'&#x20;&#x20;&#x20;&#x20;'" />

	<xsl:template name="replace">
		<xsl:param name="text" />
		<xsl:param name="old" />
		<xsl:param name="new" />
		<xsl:choose>
			<xsl:when test="contains($text, $old)">
				<xsl:value-of select="substring-before($text, $old)"/>
				<xsl:value-of select="$new"/>
				<xsl:call-template name="replace">
					<xsl:with-param name="text" select="substring-after($text, $old)" />
					<xsl:with-param name="old" select="$old" />
					<xsl:with-param name="new" select="$new" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$text"/>
			</xsl:otherwise>			
		</xsl:choose>
	</xsl:template>

	<xsl:template name="expandTabs">
		<xsl:param name="text" />
		<xsl:call-template name="replace">
			<xsl:with-param name="text" select="$text" />
			<xsl:with-param name="old" select="'&#x9;'" />
			<xsl:with-param name="new" select="$tabReplacement" />
		</xsl:call-template>
	</xsl:template>
</xsl:stylesheet>
