<?xml version='1.0' encoding='UTF-8'?>
<xsl:stylesheet version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>
<xsl:output method='json'/>
<xsl:variable name='gtlist' select='/staff/employee/name'/>
<xsl:template match='/'>
{{ "employee":
<xsl:text>  [</xsl:text>                                                        
<xsl:for-each select='$gtlist'>                                              
        <xsl:sort select='.' />                                                      
"<xsl:value-of select='.' />"                                                 
<xsl:if test='position() != last()'>,\n  </xsl:if>                             
</xsl:for-each>                                                               
<xsl:text>]\n}}</xsl:text>                                                      
</xsl:template>
</xsl:stylesheet>
