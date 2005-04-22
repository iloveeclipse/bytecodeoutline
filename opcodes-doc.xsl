<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    xmlns="http://www.w3.org/1999/xhtml" 
    exclude-result-prefixes="xsl fo">
  
<xsl:output method="html" indent="no" omit-xml-declaration="yes" encoding="UTF-8" name="html"/>
<xsl:output method="xml" indent="yes" encoding="UTF-8" name="xml"/>

<xsl:template match="opcodes">


<!-- TOC -->
<xsl:result-document format="xml" href="toc.xml">

<toc label="JVM Instruction Reference" topic="about.html">

<xsl:for-each select='opcode'>
  <xsl:sort/>
  <topic>
    <xsl:attribute name='label'><xsl:value-of select="name/text()"/></xsl:attribute>
    <xsl:attribute name='href'>doc/ref-<xsl:value-of select="name/text()"/>.html</xsl:attribute>
  </topic>
</xsl:for-each>

</toc>
</xsl:result-document>


<!-- Details -->
<xsl:apply-templates select="*">
  <xsl:sort/>
</xsl:apply-templates>

</xsl:template>

<xsl:template match="opcode">

<xsl:variable name="filename" select="concat('ref-', name, '.html')"/>
<xsl:result-document format="html" href="{$filename}">

<html>
<head>
<style type="text/css">
dt {
  font-style: italic;
  margin-top: 15px;
  margin-bottom: 3px;
  margin-left: 0px;
  border-bottom: 1px dotted black;
}
dd {
  margin-left: 10px;
}
table {
  border-collapse:collapse;
  border: 1px solid black;
  margin-top: 7px;
}
th {
  border: 1px solid black;
  padding: 3 7 3 7;
}
td {
  border: 1px solid black;
  padding: 3 7 3 7;
}
</style>  
</head>
<body>

<p>
  <a><xsl:attribute name='name'><xsl:value-of select="name"/></xsl:attribute></a>
  <b><xsl:value-of select="name"/></b> : 
  <xsl:apply-templates select="short/text()"/>
</p>
<dl>
<xsl:apply-templates select="desc"/>
<xsl:apply-templates select="exceptions"/>
<xsl:apply-templates select="example"/>
<xsl:apply-templates select="note"/>
<xsl:apply-templates select="see"/>
<xsl:apply-templates select="stack"/>
<xsl:apply-templates select="bytecode"/>
</dl>

</body>
</html>

</xsl:result-document>

</xsl:template>


<!-- Sections -->

<xsl:template match="stack">
<dt>Stack</dt>
<dd><xsl:apply-templates select="*|text()"/></dd>
</xsl:template>

<xsl:template match="desc">
<dt>Description</dt>
<dd><xsl:apply-templates select="*|text()"/></dd>
</xsl:template>

<xsl:template match="bytecode">
<dt>Bytecode</dt>
<dd><xsl:apply-templates select="*|text()"/></dd>
</xsl:template>

<xsl:template match="example">
<dt>Example</dt>
<dd><xsl:apply-templates select="*|text()"/></dd>
</xsl:template>

<xsl:template match="note">
<dt>Notes</dt>
<dd><xsl:apply-templates select="*|text()"/></dd>
</xsl:template>

<xsl:template match="see">
<dt>See also</dt>
<dd>
  <xsl:call-template name="seeFormat"><xsl:with-param name="string" select="text()"/></xsl:call-template>
  </dd>
</xsl:template>

<xsl:template match="exceptions">
<dt>Exceptions</dt>
<dd><xsl:apply-templates select="*|text()"/></dd>
</xsl:template>


<!-- Formatting -->

<xsl:template match='pre'>
  <pre><xsl:call-template name="preFormat"><xsl:with-param name="string" select="text()"/></xsl:call-template></pre>
</xsl:template>

<xsl:template match='text()'>
  <xsl:call-template name="docFormat"><xsl:with-param name="string" select="."/></xsl:call-template>
</xsl:template>

<xsl:template name="preFormat">
  <xsl:param name="string"/>
  <!-- double CRLF will be replaced with <BR><BR> -->
  <xsl:choose>      
    <xsl:when test="contains($string, '&#xA;&#xA;')">
      <xsl:value-of select="substring-before($string, '&#xA;&#xA;')"/>
      <xsl:value-of select="string( '&#xA;')"/>
      <xsl:call-template name="preFormat">
        <xsl:with-param name="string" select="substring-after($string, '&#xA;&#xA;')"/>
      </xsl:call-template>
    </xsl:when>      
    <xsl:otherwise>
      <xsl:value-of select="$string"/>
    </xsl:otherwise>
  </xsl:choose>      
</xsl:template>
    
<xsl:template name="docFormat">
  <xsl:param name="string"/>
  <!-- double CRLF will be replaced with <BR><BR> -->
  <xsl:choose>      
    <xsl:when test="contains($string, '&#xA;&#xA;')">
      <xsl:value-of select="substring-before($string, '&#xA;&#xA;')"/>
      <!-- xsl:value-of select="string( '&#xA;')"/ -->
      <br/>
      <xsl:call-template name="docFormat">
        <xsl:with-param name="string" select="substring-after($string, '&#xA;&#xA;')"/>
      </xsl:call-template>
    </xsl:when>      
    <xsl:otherwise>
      <xsl:value-of select="$string"/>
    </xsl:otherwise>
  </xsl:choose>      
</xsl:template>

<xsl:template name="seeFormat">
  <xsl:param name="string"/>
  <xsl:choose>      
    <xsl:when test="contains($string, ',')">
      <xsl:call-template name="seeFormat"><xsl:with-param name="string" select="substring-before($string, ',')"/></xsl:call-template>
      <xsl:call-template name="seeFormat"><xsl:with-param name="string" select="substring-after($string, ',')"/></xsl:call-template>
    </xsl:when>      

    <xsl:when test="contains($string, ' ')">
      <xsl:call-template name="seeFormat"><xsl:with-param name="string" select="substring-before($string, ' ')"/></xsl:call-template>
      <xsl:call-template name="seeFormat"><xsl:with-param name="string" select="substring-after($string, ' ')"/></xsl:call-template>
    </xsl:when>      

    <xsl:when test="contains($string, '&#xA;')">
      <xsl:call-template name="seeFormat"><xsl:with-param name="string" select="substring-before($string, '&#xA;')"/></xsl:call-template>
      <xsl:call-template name="seeFormat"><xsl:with-param name="string" select="substring-after($string, '&#xA;')"/></xsl:call-template>
    </xsl:when>      

    <xsl:otherwise>
      <xsl:value-of select="string( ' ')"/>
      <a>
        <xsl:attribute name="href">ref-<xsl:value-of select="normalize-space($string)"/>.html</xsl:attribute>
        <xsl:value-of select="normalize-space($string)"/>
      </a>
    </xsl:otherwise>
  </xsl:choose>      
</xsl:template>


<!-- Copy everything else -->

<xsl:template match='*|@*'>
  <xsl:copy>
    <xsl:apply-templates select='node()|@*'/>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>
