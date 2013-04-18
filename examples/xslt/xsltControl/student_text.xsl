<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="text"/>

  <xsl:template match="/">
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="student_list">
    Student Directory for example.edu
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="name">
    Name: <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="major">
    Major: <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="phone">
    Phone: <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="email">
    Email: <xsl:apply-templates />
  </xsl:template>

</xsl:stylesheet>

