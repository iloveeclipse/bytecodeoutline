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
<topic label="Opcodes" href="doc/opcodes.html"/>

<xsl:for-each select='opcode'>
  <xsl:sort/>
  <topic>
    <xsl:attribute name='label'><xsl:value-of select="name/text()"/></xsl:attribute>
    <xsl:attribute name='href'>doc/ref-<xsl:value-of select="name/text()"/>.html</xsl:attribute>
  </topic>
</xsl:for-each>

</toc>
</xsl:result-document>


<!-- opcodes -->

<xsl:result-document format="html" href="opcodes.html">
<html xmlns="http://www.w3.org/1999/xhtml">
<xsl:call-template name="head"/>
<body>
<ul>
<xsl:for-each select='opcode'>
  <xsl:sort/>

  <li><a>
    <xsl:attribute name='href'>ref-<xsl:value-of select="name/text()"/>.html</xsl:attribute>
    <xsl:value-of select="name/text()"/>
  </a></li>

</xsl:for-each>
</ul>
</body>
</html>

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
<xsl:call-template name="head"/>
<body>

<p>
  <a><xsl:attribute name='name'><xsl:value-of select="name"/></xsl:attribute></a>
  <b><xsl:value-of select="name"/></b> :
  <xsl:apply-templates select="short/text()"/>
  <xsl:value-of select="string(' : ')"/><a href="opcodes.html">index</a>
  <xsl:value-of select="string(' : ')"/>
  <xsl:variable name="nm"><xsl:value-of select="name"/></xsl:variable>
  <xsl:choose>
    <xsl:when test="contains('getstatic putstatic getfield putfield', $nm)">
      <a href="http://asm.objectweb.org/asm40/javadoc/user/org/objectweb/asm/MethodVisitor.html#visitFieldInsn(int,%20java.lang.String,%20java.lang.String,%20java.lang.String)">visitFieldInsn()</a>
    </xsl:when>
    <xsl:when test="contains('iinc', $nm)">
      <a href="http://asm.objectweb.org/asm40/javadoc/user/org/objectweb/asm/MethodVisitor.html#visitIincInsn(int,%20int)">visitIincInsn()</a>
    </xsl:when>
    <xsl:when test="contains('nop aconst_null iconst_m1 iconst_0 iconst_1 iconst_2 iconst_3 iconst_4 iconst_5 lconst_0 lconst_1 fconst_0 fconst_1 fconst_2 dconst_0 dconst_1 iaload laload faload daload aaload baload caload saload iastore lastore fastore dastore aastore bastore castore sastore pop pop2 dup dup_x1 dup_x2 dup2 dup2_x1 dup2_x2 swap iadd ladd fadd dadd isub lsub fsub dsub imul lmul fmul dmul idiv ldiv fdiv ddiv irem lrem frem drem ineg lneg fneg dneg ishl lshl ishr lshr iushr lushr iand land ior lor ixor lxor i2l i2f i2d l2i l2f l2d f2i f2l f2d d2i d2l d2f i2b i2c i2s lcmp fcmpl fcmpg dcmpl dcmpg ireturn lreturn freturn dreturn areturn return arraylength athrow monitorenter monitorexit', $nm)">
      <a href="http://asm.objectweb.org/asm40/javadoc/user/org/objectweb/asm/MethodVisitor.html#visitInsn(int)">visitInsn()</a>
    </xsl:when>
    <xsl:when test="contains('bipush sipush newarray', $nm)">
      <a href="http://asm.objectweb.org/asm40/javadoc/user/org/objectweb/asm/MethodVisitor.html#visitIntInsn(int,%20int)">visitIntInsn()</a>
    </xsl:when>
    <xsl:when test="contains('ifeq ifne iflt ifge ifgt ifle if_icmpeq if_icmpne if_icmplt if_icmpge if_icmpgt if_icmple if_acmpeq if_acmpne goto jsr ifnull ifnonnull', $nm)">
      <a href="http://asm.objectweb.org/asm40/javadoc/user/org/objectweb/asm/MethodVisitor.html#visitJumpInsn(int,%20org.objectweb.asm.Label)">visitJumpInsn()</a>
    </xsl:when>
    <xsl:when test="contains('ldc ldc_w ldc2_w', $nm)">
      <a href="http://asm.objectweb.org/asm40/javadoc/user/org/objectweb/asm/MethodVisitor.html#visitLdcInsn(java.lang.Object)">visitLdcInsn()</a>
    </xsl:when>
    <xsl:when test="contains('lookupswitch', $nm)">
      <a href="http://asm.objectweb.org/asm40/javadoc/user/org/objectweb/asm/MethodVisitor.html#visitLookupSwitchInsn(org.objectweb.asm.Label,%20int[],%20org.objectweb.asm.Label[])">visitLookupSwitchInsn()</a>
    </xsl:when>
    <xsl:when test="contains('tableswitch', $nm)">
      <a href="http://asm.objectweb.org/asm40/javadoc/user/org/objectweb/asm/MethodVisitor.html#visitTableSwitchInsn(int,%20int,%20org.objectweb.asm.Label,%20org.objectweb.asm.Label[])">visitTableSwitchInsn()</a>
    </xsl:when>
    <xsl:when test="contains('invokevirtual invokespecial invokestatic invokeinterface', $nm)">
      <a href="http://asm.objectweb.org/asm40/javadoc/user/org/objectweb/asm/MethodVisitor.html#visitMethodInsn(int,%20java.lang.String,%20java.lang.String,%20java.lang.String)">visitMethodInsn()</a>
    </xsl:when>
    <xsl:when test="contains('multianewarray', $nm)">
      <a href="http://asm.objectweb.org/asm40/javadoc/user/org/objectweb/asm/MethodVisitor.html#visitMultiANewArrayInsn(java.lang.String,%20int)">visitMultiANewArrayInsn()</a>
    </xsl:when>
    <xsl:when test="contains('new anewarray checkcast instanceof', $nm)">
      <a href="http://asm.objectweb.org/asm40/javadoc/user/org/objectweb/asm/MethodVisitor.html#visitTypeInsn(int,%20java.lang.String)">visitTypeInsn()</a>
    </xsl:when>
    <xsl:when test="contains('iload lload fload dload aload istore lstore fstore dstore astore ret', $nm)">
      <a href="http://asm.objectweb.org/asm40/javadoc/user/org/objectweb/asm/MethodVisitor.html#visitVarInsn(int,%20int)">visitVarInsn()</a>
    </xsl:when>
  </xsl:choose>
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
      <a>
        <xsl:attribute name="href">ref-<xsl:value-of select="normalize-space($string)"/>.html</xsl:attribute>
        <xsl:value-of select="normalize-space($string)"/>
      </a>
      <xsl:value-of select="string( ' ')"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<xsl:template name='head'>
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
</xsl:template>


<!-- Copy everything else -->

<xsl:template match='*|@*'>
  <xsl:copy>
    <xsl:apply-templates select='node()|@*'/>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>
