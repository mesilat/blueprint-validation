<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xhtml="http://www.w3.org/1999/xhtml"
  xmlns:ac="http://www.atlassian.com/schema/confluence/4/ac/"
  xmlns:at="http://www.atlassian.com/schema/confluence/4/at/"
  xmlns:ri="http://www.atlassian.com/schema/confluence/4/ri/"
  xmlns:acxhtml="http://www.atlassian.com/schema/confluence/4/"
  >
  <xsl:output method="xml" indent="yes" omit-xml-declaration="yes"
    cdata-section-elements="ac:plain-text-body ac:plain-text-link-body"
  />

  <xsl:template match="/">
    <ac:layout>
      <ac:layout-section ac:type="two_left_sidebar">
        <ac:layout-cell>
          <xsl:copy-of select="*"/>
        </ac:layout-cell>
        <ac:layout-cell>
          <p>
            <br/>
          </p>
        </ac:layout-cell>
      </ac:layout-section>
    </ac:layout>
  </xsl:template>

</xsl:stylesheet>
