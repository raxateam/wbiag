<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!-- 
    
    This stylesheet can be used to present calcgroup xml data in a pretty,
    human readable format. The stylesheet will transform the xml into html, and the
    following css will format it. 

    This has only been tested on IE6. It will not work on Firefox 1.5. (for some
    reason this does not support complex xslts and hangs)

    Tested with calcgroup xmls from WB 4.1

    @author Aizaz Ahmed
            Feb 8, 2006

    -->

    <xsl:template match="/">
        <head>
            <link rel="stylesheet" type="text/css" href="ruleConfigCSS.css" />
        </head>
        <body>
        <div id="container">
            <a name="top"></a>
            <h1>Pay Rules Documentation</h1>

            <!-- First make the table of contents -->
            <div class="toc">
                <h2 class="contents">Contents</h2>
            <ul>
                <xsl:for-each select="calcgroup">
                    <li>
                        <a href="#{@name}"><xsl:value-of select="@name"/></a>
                        <ul>
                            <xsl:for-each select="rule">
                                <li>
                                    <a href="#{../@name}-{@name}-{@description}">
                                        <xsl:value-of select="@name" />
                                        <xsl:text>:</xsl:text>
                                        <xsl:value-of select="@description" />
                                    </a>

                                    <ul>
                                    <xsl:for-each select="conditionset">
                                        <li>
                                            <a href="#{../../@name}-{../@name}-{../@description}-{@description}">
                                            <xsl:value-of select="@description" />
                                            </a>
                                        </li>
                                    </xsl:for-each>
                                    </ul>
                                </li>
                            </xsl:for-each>
                        </ul>
                    </li>
                </xsl:for-each>
            </ul>
            </div>

        <xsl:apply-templates/>
        </div>
        </body>
    </xsl:template>



    <xsl:template match="calcgroup">
        <div class="calcgroup">
            <div class="calcgroupinfo">
                <a name="#{@name}"></a>
                <h2>Calculation Group: <xsl:value-of select="@name"></xsl:value-of></h2>
                <xsl:if test="@description">
                    <div class="desc">
                        <span class="imp">Description: <xsl:text> </xsl:text></span>
                        <xsl:value-of select="@description"></xsl:value-of>
                    </div>
                </xsl:if>
                <span class="imp">AutoRecalc</span>: <xsl:value-of select="@autoRecalc"></xsl:value-of><xsl:text>, </xsl:text>
                <span class="imp">CalcPeriod</span>: <xsl:value-of select="@calcPeriod"></xsl:value-of>
            </div>

        <xsl:apply-templates select="rule"/>

        </div>
    </xsl:template>



    <xsl:template match="rule">

            <div class="rule">
                <a name="#{../@name}-{@name}-{@description}"></a>
                <div class="topLink"><a href="#top">Back to Top</a></div>
                <h3>Rule: <xsl:value-of select="@name"></xsl:value-of> </h3>
                <xsl:if test="@description">
                    <div class="desc">
                        <span class="imp">Description: <xsl:text> </xsl:text></span>
                        <xsl:value-of select="@description"></xsl:value-of>
                    </div>
                </xsl:if>
                <span class="imp">Active</span>: <xsl:value-of select="@isActive"></xsl:value-of> <xsl:text>, </xsl:text>
                <span class="imp">Execution Point</span>: <xsl:value-of select="@executionPoint"></xsl:value-of>

                <xsl:apply-templates select ="conditionset"/>
            </div> 
    </xsl:template>



    
    <xsl:template match="conditionset">
        <div class="conditionset">
        
            <a name="#{../../@name}-{../@name}-{../@description}-{@description}"></a>
            <span class="imp">ConditionSet</span>: <xsl:value-of select="@description"></xsl:value-of>
                <div class="parameterContainer">
                    <table width="100%" class="parameter"><tr>
                            <th>Parameter Name</th><th> Parameter Value</th>
                        </tr>
                        <xsl:apply-templates select="parameter"/>
                    </table>
                </div>
                <xsl:apply-templates select="condition"/>
        </div>
    </xsl:template>




    <xsl:template match="condition">
        <div class="condition">
        
            <span class="imp">Condition: <xsl:value-of select="@name"></xsl:value-of></span><br />
            <span class="imp">Class</span>: <xsl:value-of select="@class"></xsl:value-of>
            <xsl:if test="@description">
                <div class="desc">
                    <span class="imp">Description: <xsl:text> </xsl:text></span>
                    <xsl:value-of select="@description"></xsl:value-of>
                </div>
            </xsl:if>
            <xsl:if test="parameter">
                <div class="parameterContainer">
                    <table width ="100%" class="parameter">
                        <tr>
                            <th>Parameter Name</th><th>Parameter Value</th>
                        </tr>
                        <xsl:apply-templates select="parameter"/>
                    </table>
                </div>
            </xsl:if>
        </div>
    </xsl:template>




    <xsl:template match="parameter" >
        <tr>
            <!-- Very unfortunately, these width values needs to be placed here. 
            The second column has to have a width explicitly set, or else long lines of
            unbreakable text (as parameters often are) will break the formatting -->
            <td width="40%" ><xsl:value-of select="name"></xsl:value-of></td>
            <td width="370px"><xsl:value-of select="value"></xsl:value-of></td>
        </tr>
    </xsl:template>


    
</xsl:stylesheet>
