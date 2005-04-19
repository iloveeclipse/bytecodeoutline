<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:fo="http://www.w3.org/1999/XSL/Format" 
    exclude-result-prefixes="xsl fo">
  
<xsl:output method="html" indent="no" omit-xml-declaration="yes" encoding="UTF-8"/>

<xsl:template match="opcodes">
<html>
<xsl:comment>
</xsl:comment>
<body>

<!-- TOC -->
<xsl:for-each select='opcode'>
  <xsl:sort/>
  <xsl:variable name='nm' select='name/text()'/>
  <a><xsl:attribute name='href'>#<xsl:value-of select="name/text()"/></xsl:attribute><xsl:value-of select="name/text()"/></a>
  <xsl:value-of select="string( ' ')"/>
</xsl:for-each>
<hr/>

<!-- Details -->
<xsl:apply-templates select="*">
  <xsl:sort/>
</xsl:apply-templates>
</body>
</html>

</xsl:template>

<xsl:template match="opcode">
<dl>
<dt>
  <a><xsl:attribute name='name'><xsl:value-of select="name"/></xsl:attribute></a>
  <b><xsl:value-of select="name"/></b> : 
  <xsl:apply-templates select="short/text()"/>
  </dt>
<xsl:apply-templates select="stack"/>
<xsl:apply-templates select="desc"/>
<xsl:apply-templates select="exceptions"/>
<xsl:apply-templates select="example"/>
<xsl:apply-templates select="bytecode"/>
<xsl:apply-templates select="note"/>
<xsl:apply-templates select="see"/>
</dl>
<hr/>

</xsl:template>


<!-- Sections -->

<xsl:template match="stack">
<dt><i>Stack</i></dt>
<dd><xsl:apply-templates select="*|text()"/></dd>
</xsl:template>

<xsl:template match="desc">
<dt><i>Description</i></dt>
<dd><xsl:apply-templates select="*|text()"/></dd>
</xsl:template>

<xsl:template match="bytecode">
<dt><i>Bytecode</i></dt>
<dd><xsl:apply-templates select="*|text()"/></dd>
</xsl:template>

<xsl:template match="example">
<dt><i>Example</i></dt>
<dd><xsl:apply-templates select="*|text()"/></dd>
</xsl:template>

<xsl:template match="note">
<dt><i>Notes</i></dt>
<dd><xsl:apply-templates select="*|text()"/></dd>
</xsl:template>

<xsl:template match="see">
<dt><i>See also</i></dt>
<dd>
  <xsl:call-template name="seeFormat"><xsl:with-param name="string" select="text()"/></xsl:call-template>
  </dd>
</xsl:template>

<xsl:template match="exceptions">
<dt><i>Exceptions</i></dt>
<dd><xsl:apply-templates select="*|text()"/></dd>
</xsl:template>


<!-- Formatting -->

<xsl:template match="table">
  <table border="1" cellspacing="0" cellpadding="3">
  <xsl:apply-templates select="*"/>
  </table>
</xsl:template>

<xsl:template match='text()'>
  <xsl:call-template name="docFormat"><xsl:with-param name="string" select="."/></xsl:call-template>
</xsl:template>
    
<xsl:template name="docFormat">
  <xsl:param name="string"/>
  <!-- double CRLF will be replaced with <BR><BR> -->
  <xsl:choose>      
    <xsl:when test="contains($string, '&#xA;&#xA;')">
      <xsl:value-of select="substring-before($string, '&#xA;&#xA;')"/>
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
      <a>
        <xsl:attribute name="href">#<xsl:value-of select="normalize-space(substring-before($string, ','))"/></xsl:attribute>
        <xsl:value-of select="normalize-space(substring-before($string, ','))"/>
      </a>
      <xsl:value-of select="string( ' ')"/>
      <xsl:call-template name="seeFormat">
        <xsl:with-param name="string" select="substring-after($string, ',')"/>
      </xsl:call-template>
    </xsl:when>      
    <xsl:otherwise>
      <a>
        <xsl:attribute name="href">#<xsl:value-of select="normalize-space($string)"/></xsl:attribute>
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
