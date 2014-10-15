<xsl:transform version="2.0"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
               xmlns:str="http://exslt.org/strings" 
               xmlns:svg="http://www.w3.org/2000/svg" 
               xmlns:xlink="http://www.w3.org/1999/xlink"
               xmlns:fx="http://javafx.com/fxml"
               xmlns:math="http://exslt.org/math"
               extension-element-prefixes="math"
>
    <xsl:strip-space elements="*"/>


    <xsl:template match="/">
        <xsl:processing-instruction name="import">javafx.scene.*</xsl:processing-instruction>
        <xsl:processing-instruction name="import">javafx.scene.shape.*</xsl:processing-instruction>
        <xsl:processing-instruction name="import">javafx.scene.paint.*</xsl:processing-instruction>
        <xsl:processing-instruction name="import">javafx.scene.image.*</xsl:processing-instruction>
        <xsl:processing-instruction name="import">javafx.scene.transform.*</xsl:processing-instruction>
        <xsl:processing-instruction name="import">javafx.scene.effect.*</xsl:processing-instruction>
        <xsl:processing-instruction name="import">javafx.scene.text.*</xsl:processing-instruction>
        <Group id="Document" xmlns:fx="http://javafx.com/fxml">
            <children>
                <xsl:apply-templates/>
            </children>
        </Group>
    </xsl:template>
     
    <xsl:template match="svg:defs">
        <xsl:element name="fx:define">
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template> 
    
    <xsl:template match="svg:clipPath">
        <Group>
            <xsl:if test="@id != ''">
                <xsl:attribute name="fx:id">
                    <xsl:value-of select="@id"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@transform != ''">
                <xsl:call-template name="transform">
                    <xsl:with-param name="transform" select="@transform"/>
                </xsl:call-template>           
            </xsl:if>
            <children>   
                <xsl:apply-templates/>
            </children>
        </Group>
    </xsl:template>
    
    <xsl:template match="svg:filter">
        <xsl:variable name = "id">
            <xsl:value-of select="@id"/>
        </xsl:variable>
        <xsl:choose>
           <!-- Fireworks defines Drop Shadows like this -->
           <xsl:when test="normalize-space(comment())='Drop Shadow'">
               <xsl:element name="DropShadow">
                        <xsl:if test="$id">
                            <xsl:attribute name="fx:id">
                                <xsl:value-of select="$id"/>
                            </xsl:attribute>
                        </xsl:if>
                        <xsl:if test="./svg:feOffset">
                            <xsl:attribute name="offsetX">
                               <xsl:value-of select="./svg:feOffset/@dx"/>

                            </xsl:attribute>
                            <xsl:attribute name="offsetY">
                               <xsl:value-of select="./svg:feOffset/@dy"/>
                            </xsl:attribute>
                        </xsl:if>
                        <xsl:if test="./svg:feGaussianBlur">
                            <xsl:attribute name="blurType">
                                <xsl:value-of select="'GAUSSIAN'"/>
                            </xsl:attribute>
                            <xsl:attribute name="radius">
                                <xsl:value-of select="number(./svg:feGaussianBlur/@stdDeviation)*2"/>
                            </xsl:attribute>
                        </xsl:if>
               </xsl:element>
           </xsl:when>

           <xsl:when test="./svg:feGaussianBlur">
               <xsl:element name="GaussianBlur">
                <xsl:if test="$id">
                    <xsl:attribute name="fx:id">
                        <xsl:value-of select="$id"/>
                    </xsl:attribute>
                </xsl:if>
                <xsl:element name="radius">
                    <xsl:value-of select="number(./svg:feGaussianBlur/@stdDeviation)*2"/>
                </xsl:element>
            </xsl:element>
           </xsl:when>
        </xsl:choose>
        
         
         <!--<xsl:for-each select="./*">
           
            <xsl:when test="name()='feOffset'">
                <xsl:variable name="comment">
                    
                    <xsl:value-of select="preceding-sibling::node()[not(self::text()[normalize-space(.)=''])][1][self::comment()]" />
                </xsl:variable>
                <xsl:if test="normalize-space($comment)='Drop Shadow'">
                    
            </xsl:if>
        </xsl:when>
        <xsl:otherwise test="name()='feGaussianBlur'">        
            <xsl:element name="GaussianBlur">
                <xsl:if test="$id">
                    <xsl:attribute name="fx:id">
                        <xsl:value-of select="$id"/>
                    </xsl:attribute>
                </xsl:if>
                <xsl:element name="radius">
                    <xsl:value-of select="number(@stdDeviation)*2"/>
                </xsl:element>
            </xsl:element>
        </xsl:otherwise>
        </xsl:choose>
        -->
    </xsl:template>
    
 
    
    <xsl:template name="effect">
        <xsl:param name="gaussianBlur"/>       
        <xsl:element name="radius">
            <xsl:value-of select="number($gaussianBlur/svg:feGaussianBlur/@stdDeviation)*2"/>
        </xsl:element>
    </xsl:template> 
    
    <xsl:template match="svg:metadata"><!--hack to get rid of "image/svg+xml" -->
    </xsl:template>
    <xsl:template match="svg:g">
        <Group>
            <xsl:if test="@style != ''">
                <xsl:call-template name="group-style">
                    <xsl:with-param name="style" select="@style"/>
                </xsl:call-template>   
            </xsl:if> 
            <xsl:if test="@id != ''">
                <xsl:attribute name="id">
                    <xsl:value-of select="@id"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@transform != ''">
                <xsl:call-template name="transform">
                    <xsl:with-param name="transform" select="@transform"/>
                </xsl:call-template>           
            </xsl:if>
            <children>   
                <xsl:apply-templates/>
            </children>
        </Group>
    </xsl:template>
    
    
    <xsl:template match="svg:text"><!--todo-->
        <xsl:element name="Text">
            <xsl:if test="@id != ''">
                <xsl:attribute name="id">
                    <xsl:value-of select="@id"/>
                </xsl:attribute>
            </xsl:if>   
            <xsl:if test="@x != ''">
                <xsl:attribute name="x">
                    <xsl:value-of select="@x"/>
                </xsl:attribute>
            </xsl:if>   
            <xsl:if test="@y != ''">
                <xsl:attribute name="y">
                    <xsl:value-of select="@y"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@style != ''">
                <xsl:call-template name="style">
                    <xsl:with-param name="style" select="@style"/>
                </xsl:call-template>   
            </xsl:if> 
            <xsl:if test="@transform != ''">
                <xsl:call-template name="transform">
                    <xsl:with-param name="transform" select="@transform"/>
                </xsl:call-template>           
            </xsl:if>
            <xsl:element name="text">
                <xsl:value-of select="./svg:tspan"/>
            </xsl:element>  
        </xsl:element>
    </xsl:template> 
    
    <xsl:template match="svg:use">
        <Group>
           
            <xsl:if test="@style != ''">
                <xsl:call-template name="style">
                    <xsl:with-param name="style" select="@style"/>
                </xsl:call-template>   
            </xsl:if> 
            <xsl:if test="@id != ''">
                <xsl:attribute name="id">
                    <xsl:value-of select="@id"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@transform != ''">
                <xsl:call-template name="transform">
                    <xsl:with-param name="transform" select="@transform"/>
                </xsl:call-template>           
            </xsl:if>
            <xsl:if test="./@xlink:href != ''">
                <xsl:variable name="id"> 
                    <xsl:value-of  select="substring-after(./@xlink:href,'#')"/>
                </xsl:variable>
                <children>
                    <xsl:apply-templates select="//*[@id=$id]"/>
                </children>
            </xsl:if>
            
        </Group>
    </xsl:template>
    
    <xsl:template match="svg:rect">
        <Rectangle>
            <xsl:if test="@x!=''">
                <xsl:attribute name="x">
                    <xsl:value-of select="@x"/>
                </xsl:attribute>
            </xsl:if>   
            <xsl:if test="@y!=''">
                <xsl:attribute name="y">
                    <xsl:value-of select="@y"/>
                </xsl:attribute>
            </xsl:if>   
            <xsl:if test="@width!=''">
                <xsl:attribute name="width">
                    <xsl:value-of select="@width"/>
                </xsl:attribute>
            </xsl:if>   
            <xsl:if test="@height!=''">
                <xsl:attribute name="height">
                    <xsl:value-of select="@height"/>
                </xsl:attribute>
            </xsl:if> 
            <xsl:call-template name="element">
                <xsl:with-param name="element">
                    <xsl:value-of select="."/>
                </xsl:with-param>
            </xsl:call-template>
        </Rectangle>
    </xsl:template>
    
    <xsl:template match="svg:path">
        <SVGPath>
            <xsl:if test="@d!=''">
                <xsl:attribute name="content">
                    <xsl:value-of select="@d"/>
                </xsl:attribute>  
            </xsl:if>
            <xsl:call-template name="element">
                <xsl:with-param name="element">
                    <xsl:value-of select="."/>
                </xsl:with-param>
            </xsl:call-template>
        </SVGPath>
    </xsl:template>
    
    <xsl:template match="svg:polygon">
        <Polygon>
            <xsl:if test="@points!=''">
                <xsl:attribute name="points">
                    <xsl:variable name="comma">,</xsl:variable>
                    <xsl:for-each select="str:split(normalize-space(@points),',')">
                    
                        <xsl:choose>
                            <xsl:when test="contains(normalize-space(.),' ')">
                                <xsl:variable name="coordinates" select="str:split(normalize-space(.),' ')"/>
                                <xsl:value-of select="normalize-space($coordinates[1])"/>
                                <xsl:value-of select="$comma"/>
                                <xsl:value-of select="normalize-space($coordinates[2])"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="normalize-space(.)"/>
                            </xsl:otherwise>
                        </xsl:choose>
                        <xsl:if test="position()!=last()">
                            <xsl:value-of select="$comma"/>
                        </xsl:if>
                    </xsl:for-each>
                
                </xsl:attribute>  
            </xsl:if>
            <xsl:call-template name="element">
                <xsl:with-param name="element">
                    <xsl:value-of select="."/>
                </xsl:with-param>
            </xsl:call-template>
        </Polygon>
    </xsl:template>
    <xsl:template match="svg:circle">
        <Circle>
            <xsl:if test="@cx!=''">
                <xsl:attribute name="centerX">
                    <xsl:value-of select="@cx"/>
                </xsl:attribute>          
            </xsl:if>
            <xsl:if test="@cy!=''">
                <xsl:attribute name="centerY">
                    <xsl:value-of select="@cy"/>
                </xsl:attribute>          
            </xsl:if>
            <xsl:if test="@r!=''">
                <xsl:attribute name="radius">
                    <xsl:value-of select="@r"/>
                </xsl:attribute>          
            </xsl:if>
            <xsl:call-template name="element">
                <xsl:with-param name="element">
                    <xsl:value-of select="."/>
                </xsl:with-param>
            </xsl:call-template>
        </Circle>
    </xsl:template>
    
    <xsl:template name="element">
        <xsl:param name="element"/>
        <xsl:if test="@id != ''">
            <xsl:attribute name="id">
                <xsl:value-of select="@id"/>
            </xsl:attribute>
        </xsl:if>  
        
        <xsl:variable name="style">
            <xsl:if test="@fill!=''">
                <xsl:value-of select="concat(concat('fill:',@fill),';')"/>
            </xsl:if> 
            <xsl:if test="@stroke!=''">
                <xsl:value-of select="concat(concat('stroke:',@stroke),';')"/>
            </xsl:if>
            <xsl:if test="@stroke-width!=''">
                <xsl:value-of select="concat(concat('stroke-width:',@stroke-width),';')"/>
            </xsl:if>
            <xsl:if test="@filter!=''">
                <xsl:value-of select="concat(concat('filter:',@filter),';')"/>
            </xsl:if>
        </xsl:variable>                   
                
        <xsl:if test="@style != ''">
            <xsl:call-template name="style">
                <xsl:with-param name="style" select="@style"/>
            </xsl:call-template>           
        </xsl:if>
                
        <xsl:if test="$style != ''">
            <xsl:call-template name="style">
                <xsl:with-param name="style" select="$style"/>
            </xsl:call-template>           
        </xsl:if>
        <xsl:if test="@transform != ''">
            <xsl:call-template name="transform">
                <xsl:with-param name="transform" select="@transform"/>
            </xsl:call-template>           
        </xsl:if>
        <xsl:if test="@clip-path != ''">
            <xsl:call-template name="clip">
                <xsl:with-param name="clipPath" select="@clip-path"/>
            </xsl:call-template>           
        </xsl:if>
        
    </xsl:template>
    
    <xsl:template name="color">
        <xsl:param name="hexColor" />
        <red>
            <xsl:variable name="dec">
                <xsl:call-template name="HexToDecimal">
                    <xsl:with-param name="hexNumber" select="substring($hexColor,2,2)"/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:value-of select="number($dec) div 255"/>
        </red>
        <green>
            <xsl:variable name="dec">
                <xsl:call-template name="HexToDecimal">
                    <xsl:with-param name="hexNumber" select="substring($hexColor,4,2)"/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:value-of select="number($dec) div 255"/>
        </green>
        <blue>
            <xsl:variable name="dec">
                <xsl:call-template name="HexToDecimal">
                    <xsl:with-param name="hexNumber" select="substring($hexColor,6,2)"/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:value-of select="number($dec) div 255"/>
        </blue>
    </xsl:template>
    
    <xsl:template name="transform">
        <xsl:param name="transform" />
        <transforms>
            <xsl:for-each select="str:split($transform, ';')">
                <xsl:if test="substring-before(.,'(')='translate'">
                    <xsl:variable name="coordinates" select="substring-before(substring-after(.,'translate('),')')"/>
                     
                    <xsl:element name="Translate">
                        <xsl:attribute name="x"> 
                            <xsl:value-of select="substring-before($coordinates,',')"/>
                        </xsl:attribute>
                        <xsl:attribute name="y"> 
                            <xsl:value-of select="substring-after($coordinates,',')"/>
                        </xsl:attribute>
                    </xsl:element>  
                </xsl:if>
                <xsl:if test="substring-before(.,'(')='scale'">
                    <xsl:variable name="coordinates" select="substring-before(substring-after(.,'scale('),')')"/>
                
                    <xsl:element name="Scale">
                        <xsl:attribute name="x"> 
                            <xsl:value-of select="substring-before($coordinates,',')"/>
                        </xsl:attribute>
                        <xsl:attribute name="y"> 
                            <xsl:value-of select="substring-after($coordinates,',')"/>
                        </xsl:attribute>
                    </xsl:element>  
                </xsl:if>
                <xsl:if test="substring-before(.,'(')='matrix'">
                    <xsl:variable name="coordinates" select="str:split(substring-before(substring-after(.,'matrix('),')'),',')"/>
                   
                    <!-- some examples don't use comma but whitespace as delimiter-->
                    <xsl:choose> 
                        <xsl:when 
                            test="contains(substring-before(substring-after(.,'matrix('),')'),',')">
                            <xsl:call-template name="affine">
                                <xsl:with-param name="coordinates" select="str:split(substring-before(substring-after(.,'matrix('),')'),',')"/>
                            </xsl:call-template>
                        </xsl:when>
                        <!--assume we've got whitespace demimiters-->
                        <xsl:otherwise>
                            <xsl:call-template name="affine">
                                <xsl:with-param name="coordinates" select="str:split(substring-before(substring-after(.,'matrix('),')'),' ')"/>
                            </xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>
                    
                </xsl:if>
            </xsl:for-each>
        </transforms>
    </xsl:template>
    
    <xsl:template name="affine">
        <xsl:param name="coordinates"/>
        <xsl:element name="Affine">
            <xsl:attribute name="mxx"> 
                <xsl:value-of select="$coordinates[1]"/>
            </xsl:attribute>
            <xsl:attribute name="myx"> 
                <xsl:value-of select="$coordinates[2]"/>
            </xsl:attribute>
            <xsl:attribute name="mxy"> 
                <xsl:value-of select="$coordinates[3]"/>
            </xsl:attribute>
            <xsl:attribute name="myy"> 
                <xsl:value-of select="$coordinates[4]"/>
            </xsl:attribute>
            <xsl:attribute name="tx"> 
                <xsl:value-of select="$coordinates[5]"/>
            </xsl:attribute>
            <xsl:attribute name="ty"> 
                <xsl:value-of select="$coordinates[6]"/>
            </xsl:attribute>
        </xsl:element>  
    </xsl:template>
    
    <!--       style="font-style:normal;font-variant:normal;font-weight:normal;font-stretch:normal;text-align:start;line-height:100%;writing-mode:lr-tb;text-anchor:start"
    -->
    <!-- svg supports a lot of stuff on group level that FXML doesn't,
    so we need separate methods, otherwise we get invalid properties-->
    <xsl:template name="group-style">
        <xsl:param name="style"/>
        <xsl:variable name="opacity">
            <xsl:choose>
                <xsl:when test="contains($style,'opacity:')">
                    <xsl:choose>
                        <xsl:when test="contains(substring-after($style,'opacity:'),';')">
                            <xsl:value-of select="substring-before(substring-after($style,'opacity:'),';')"></xsl:value-of>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="substring-after($style,'opacity:')"></xsl:value-of>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <opacity>
            <xsl:value-of select="$opacity"/>
        </opacity>
        <xsl:for-each select="str:split($style, ';')">        
            <xsl:if test="substring-before(.,':')='filter'">      
                <xsl:variable name="id">                        
                    <xsl:value-of select="substring-before( substring-after(.,'url(#'),')')"/>
                </xsl:variable>  
                               
                <xsl:element name="effect">
                    <xsl:element name="fx:reference">
                        <xsl:attribute name="source">
                            <xsl:value-of select="$id"/>
                        </xsl:attribute>
                    </xsl:element>
                    <!--    <xsl:element name="GaussianBlur">
                        <xsl:call-template name="effect">
                            <xsl:with-param name="gaussianBlur" select="//*[@id=$id]"/>
                        </xsl:call-template> 
                    </xsl:element>-->
                </xsl:element>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
    
    <xsl:template name="style">
        <xsl:param name="style"/>
        <xsl:variable name="opacity">
            <xsl:choose>
                <xsl:when test="contains($style,'opacity:')">
                    <xsl:choose>
                        <xsl:when test="contains(substring-after($style,'opacity:'),';')">
                            <xsl:value-of select="substring-before(substring-after($style,'opacity:'),';')"></xsl:value-of>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="substring-after($style,'opacity:')"></xsl:value-of>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                
                <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <opacity>
            <xsl:value-of select="$opacity"/>
        </opacity>
        <xsl:for-each select="str:split($style, ';')">
            <xsl:if test="substring-before(.,':')='font-size'">
                <xsl:element name="font">
                    <xsl:element name="Font">
                        <xsl:attribute name="size">
                            <xsl:value-of select="substring-before(substring-after(.,':'),'px')"/>
                        </xsl:attribute>
                        <xsl:if test="contains($style,'font-family')">
                            
                            <xsl:attribute name="name">
                                <xsl:choose>
                                    <xsl:when test="contains(substring-after($style,'font-family:'),';')">
                                        <xsl:value-of select="substring-before(substring-after($style,'font-family:'),';')"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="substring-after($style,'font-family:')"></xsl:value-of>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:attribute>
                        </xsl:if>

                    </xsl:element>
                </xsl:element>
            </xsl:if>
            <!--stroke-dasharray:none;-->
            <xsl:if test="substring-before(.,':')='filter'">
               
                <xsl:variable name="id">                        
                    <xsl:value-of select="substring-before( substring-after(.,'url(#'),')')"/>
                </xsl:variable>
                
                              
                                                          
                <xsl:element name="effect">
                    <xsl:element name="fx:reference">
                        <xsl:attribute name="source">
                            <xsl:value-of select="$id"/>
                        </xsl:attribute>
                    </xsl:element>
                    <!--
                      <xsl:element name="GaussianBlur">
                          <xsl:call-template name="effect">
                              <xsl:with-param name="gaussianBlur" select="//*[@id=$id]"/>
                          </xsl:call-template> 
                      </xsl:element>
                    -->
                </xsl:element>
            </xsl:if>
            <xsl:if test="substring-before(.,':')='stroke-dasharray'">
                <!-- <xsl:if test="substring-after(.,':')!='none'">
                    <xsl:element name="strokeDashArray">
                        <xsl:value-of select="translate(substring-after(.,'stroke-dasharray:'),$smallcase, $uppercase)"/>              
                    </xsl:element>            
                </xsl:if> -->
            </xsl:if>
            <xsl:if test="substring-before(.,':')='stroke-width'">
                <xsl:element name="strokeWidth">
                    <xsl:choose>
                        <xsl:when test="contains(substring-after(.,'stroke-width:'),'px')">
                            <xsl:value-of select="substring-before(substring-after(.,'stroke-width:'),'px')"/>          
                        </xsl:when>
                        <xsl:otherwise>    
                            <xsl:value-of select="substring-after(.,'stroke-width:')"/>          
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>
            </xsl:if>
            <xsl:if test="substring-before(.,':')='stroke-dashoffset'">
                <xsl:element name="strokeDashOffset">
                    <xsl:value-of select="substring-after(.,'stroke-dashoffset:')"/>              
                </xsl:element>
            </xsl:if>
            <xsl:if test="substring-before(.,':')='stroke-linecap'">
                <xsl:element name="strokeLineCap">
                    <xsl:value-of select="translate(substring-after(.,'stroke-linecap:'),$smallcase, $uppercase)"/>              
                </xsl:element>
            </xsl:if>
            <xsl:if test="substring-before(.,':')='stroke-linejoin'">
                <xsl:element name="strokeLineJoin">
                    <xsl:value-of select="translate(substring-after(.,'stroke-linejoin:'),$smallcase, $uppercase)"/>              
                </xsl:element>
            </xsl:if>
            <xsl:if test="substring-before(.,':')='stroke-miterlimit'">
                <xsl:element name="strokeMiterLimit">
                    <xsl:value-of select="translate(substring-after(.,'stroke-miterlimit:'),$smallcase, $uppercase)"/>              
                </xsl:element>
            </xsl:if>
            <xsl:if test="substring-before(.,':')='stroke'">
                <stroke>
                    <xsl:if test="contains(.,':#')">
                        <Color>
                            <xsl:call-template name="color">
                                <xsl:with-param name="hexColor" select="substring-after(.,':')" />
                            </xsl:call-template>
                            
                            <xsl:if test="contains($style,'stroke-opacity:')">
                                <opacity>
                                    <xsl:choose>
                                        <xsl:when test="contains(substring-after($style,'stroke-opacity:'),';')">
                                            <xsl:value-of select="number(substring-before(substring-after($style,'stroke-opacity:'),';')) * number($opacity)"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="substring-after($style,'stroke-opacity:')"></xsl:value-of>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                
                                </opacity>

                            </xsl:if>
                        </Color>
                    </xsl:if>
                    <xsl:if test="contains(substring-after(.,':'),'url')">
                        <xsl:variable name="id">                        
                            <xsl:value-of select="substring-before( substring-after(.,'url(#'),')')"/>
                        </xsl:variable>
                        <!--  <xsl:value-of select="/svg/defs/id(substring-before( substring-after(.,'url(#'),')'))/@x1"/>-->
                        <xsl:if test="contains($id,'linearGradient')">
                            <xsl:element name="LinearGradient">
                                <xsl:call-template name="gradient">
                                    <xsl:with-param name="gradient" select="//*[@id=$id]"/>
                                    <xsl:with-param name="type" >linear</xsl:with-param>                                   
                                    <xsl:with-param name="opacity" select="$opacity"/>
                                </xsl:call-template>                      
                            </xsl:element>
                        </xsl:if>
                        <xsl:if test="contains($id,'gradient')">
                            <xsl:element name="RadialGradient">
                                <xsl:call-template name="gradient">
                                    <xsl:with-param name="gradient" select="//*[@id=$id]"/>
                                    <xsl:with-param name="type" >radial</xsl:with-param>                                   
                                    <xsl:with-param name="opacity" select="$opacity"/>
                                </xsl:call-template>                      
                            </xsl:element>
                        </xsl:if>
                    </xsl:if>
                   
                    <xsl:if test="contains(substring-after(.,':'),'none')">TRANSPARENT</xsl:if>
                </stroke>
                   
            </xsl:if>
            <xsl:if test="substring-before(.,':')='fill'">
                <xsl:element name="fill">
                    <xsl:if test="contains(.,':#')">
                        <xsl:element name="Color">
                            <xsl:call-template name="color">
                                <xsl:with-param name="hexColor" select="substring-after(.,':')" />
                            </xsl:call-template>
                            <opacity>
                                <xsl:choose> 
                                    <xsl:when test="contains($style,'fill-opacity')">
                                        <xsl:value-of select="number(substring-before(substring-after($style,'fill-opacity:'),';')) * number($opacity)"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="$opacity"/>
                                    </xsl:otherwise>  
                                </xsl:choose>
                            </opacity>
                        </xsl:element>
                    </xsl:if>
                    <xsl:if test="contains(substring-after(.,':'),'url')">
                        <xsl:variable name="id">                        
                            <xsl:value-of select="substring-before( substring-after(.,'url(#'),')')"/>
                        </xsl:variable>
                        <!--  <xsl:value-of select="/svg/defs/id(substring-before( substring-after(.,'url(#'),')'))/@x1"/>-->
                        <xsl:if test="contains($id,'linearGradient')">
                            <xsl:element name="LinearGradient">
                                <xsl:call-template name="gradient">
                                    <xsl:with-param name="gradient" select="//*[@id=$id]"/>
                                    <xsl:with-param name="type" >linear</xsl:with-param>                                   
                                    <xsl:with-param name="opacity" select="$opacity"/>

                                </xsl:call-template>                      
                            </xsl:element>
                        </xsl:if>
                        <xsl:if test="contains($id,'radialGradient')">
                            <xsl:element name="RadialGradient">
                                <xsl:call-template name="gradient">
                                    <xsl:with-param name="gradient" select="//*[@id=$id]"/>
                                    <xsl:with-param name="type" >radial</xsl:with-param>  
                                    <xsl:with-param name="opacity" select="$opacity"/>

                                </xsl:call-template>                      
                            </xsl:element>
                        </xsl:if>
                    </xsl:if>
                   
                    <xsl:if test="contains(substring-after(.,':'),'none')">
                        TRANSPARENT
                    </xsl:if>
                </xsl:element>
            </xsl:if>
            
        </xsl:for-each>
 
    </xsl:template>
    
    <xsl:template name="clip">
        <xsl:param name="clipPath" />
        <xsl:element name="clip">
            <xsl:if test="contains($clipPath,'url(')">
                <!--clip-path="url(#clipPath3055)"-->
                <xsl:variable name="url">
                    <xsl:value-of select="substring-before(substring-after($clipPath,'url(#'),')')"/>
                </xsl:variable>
                <xsl:element name="fx:reference">
                    <xsl:attribute name="source">
                        <xsl:value-of select="$url"/>
                    </xsl:attribute>
                </xsl:element>
            </xsl:if>
        </xsl:element>
    </xsl:template>   
    
    <xsl:template name="stops">
        <xsl:param name="gradient" />
        <xsl:param name="type" />
        <xsl:param name="opacity" />
        <xsl:if test="$gradient/svg:stop">
            <stops>
                <xsl:for-each select="$gradient/svg:stop">
                    <xsl:element name="Stop">
                        <xsl:attribute name="offset"> 
                            <xsl:value-of  select="./@offset"/>
                        </xsl:attribute>
                        <color>
                            <xsl:element name="Color">
                                <xsl:call-template name="color" >                 
                                    <xsl:with-param name="hexColor" select="substring-after(./@style,'stop-color:')"/>
                                </xsl:call-template>
                                <xsl:choose>
                                    <xsl:when test="contains(./@style,'stop-opacity')">
                                        <opacity>
                                            <xsl:choose>
                                                <xsl:when test="contains(substring-after(@style,'stop-opacity:'),';')">
                                                    <xsl:value-of select="substring-before(substring-after(@style,'stop-opacity:'),';')"></xsl:value-of>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:value-of select="substring-after(@style,'stop-opacity:')"></xsl:value-of>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </opacity>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <opacity>
                                            <xsl:value-of select="$opacity"/>
                                        </opacity>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:element>
                        </color>          
                    </xsl:element>
                </xsl:for-each>
            </stops>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="gradient">
        <xsl:param name="gradient" />
        <xsl:param name="type" />
        <xsl:param name="opacity" />
        <xsl:variable name="matrix-string">          
            <xsl:choose>
                <xsl:when test="$gradient/@gradientTransform !=''">
                    <xsl:choose>
                        <xsl:when test="contains($gradient/@gradientTransform,'translate(')">
                            <xsl:variable name="tx">
                                <xsl:value-of 
                                    select="substring-before( substring-after($gradient/@gradientTransform,'translate('),',')"/>
                            </xsl:variable>
                            <xsl:variable name="ty">
                                <xsl:value-of 
                                    select="substring-before( substring-after($gradient/@gradientTransform,','),')')"/>
                            </xsl:variable>
                            1,0,0,1,
                            <xsl:value-of select="$tx"/>,
                            <xsl:value-of select="$ty"/>
                        </xsl:when>
                        <xsl:when test="contains($gradient/@gradientTransform,'scale(')">
                            <xsl:variable name="mxx">
                                <xsl:value-of 
                                    select="substring-before( substring-after($gradient/@gradientTransform,'scale('),',')"/>
                            </xsl:variable>
                            <xsl:variable name="mxy">
                                <xsl:value-of 
                                    select="substring-before( substring-after($gradient/@gradientTransform,','),')')"/>
                            </xsl:variable>
                            <xsl:value-of select="$mxx"/>,0,0,
                            <xsl:value-of select="$mxy"/>,0,0
                        </xsl:when>
                        <xsl:when test="contains($gradient/@gradientTransform,'matrix(')">
                            <xsl:value-of select="substring-before( substring-after($gradient/@gradientTransform,'matrix('),')')"/>
                        </xsl:when>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    1,0,0,1,0,0
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>       
        <xsl:if test="$type='radial'">
            <xsl:if test="$gradient/@r !=''">
                <xsl:element name="radius">
                    <xsl:variable name="coordinates" select="str:split($matrix-string,',')"/>                 

                    <xsl:value-of select="number($gradient/@r)*number($coordinates[1])"/>
                </xsl:element>
            </xsl:if>
            <xsl:if test="$gradient/@cx !=''">
                <proportional>false</proportional>
                <cycleMethod>NO_CYCLE</cycleMethod>
                <xsl:variable name="x1">
                    <xsl:call-template name="matrix-transform-x">
                        <xsl:with-param name="x" select="$gradient/@cx" />
                        <xsl:with-param name="y" select="$gradient/@cy" />
                        <xsl:with-param name="matrix-string" select="$matrix-string" />
                    </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="y1">
                    <xsl:call-template name="matrix-transform-y">
                        <xsl:with-param name="x" select="$gradient/@cx" />
                        <xsl:with-param name="y" select="$gradient/@cy" />
                        <xsl:with-param name="matrix-string" select="$matrix-string" />
                    </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="x2">
                    <xsl:choose>
                        <xsl:when test="$gradient/@fx !=''">
                            <xsl:call-template name="matrix-transform-x">
                                <xsl:with-param name="x" select="$gradient/@fx" />
                                <xsl:with-param name="y" select="$gradient/@fy" />
                                <xsl:with-param name="matrix-string" select="$matrix-string" />
                            </xsl:call-template>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$x1"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="y2">
                    <xsl:choose>
                        <xsl:when test="$gradient/@fy !=''">
                            <xsl:call-template name="matrix-transform-y">
                                <xsl:with-param name="x" select="$gradient/@fx" />
                                <xsl:with-param name="y" select="$gradient/@fy" />
                                <xsl:with-param name="matrix-string" select="$matrix-string" />
                            </xsl:call-template>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$y1"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:element name="centerX">
                    <xsl:value-of select="$x1"/>
                </xsl:element>
                <xsl:element name="centerY">
                    <xsl:value-of select="$y1"/>
                </xsl:element>
                <xsl:element name="focusAngle">
                    <xsl:variable name="atan">
                        <xsl:value-of  
                            select="math:atan2(number($y2)-number($y1), number($x2)-number($x1))" />
                    </xsl:variable>
                    <!--(y2 - y1), (x2 - x1)) * 180 / (3.14159265358979323846)number($atan) * 180 * -->
                
                    <xsl:value-of  
                        select="number($atan)*180*0.31830989" />
            
                </xsl:element>
                <xsl:element name="focusDistance">
                    <!--(((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1)))-->
                    <xsl:value-of  
                        select="math:sqrt(((number($x2)-number($x1))*(number($x2)-number($x1)))+((number($y2)-number($y1))*(number($y2)-number($y1))))" />
                </xsl:element>
            
            </xsl:if>
        
            
        </xsl:if>
        <xsl:if test="$gradient/@xlink:href != ''">
            <xsl:variable name="id"> 
                <xsl:value-of  select="substring-after($gradient/@xlink:href,'#')"/>
            </xsl:variable>
            <xsl:call-template name="stops">     
                <xsl:with-param name="gradient" select="//*[@id=$id]"/>
                <xsl:with-param name="type" select="$type"/>
                <xsl:with-param name="opacity" select="$opacity"/>
            </xsl:call-template>                      
        </xsl:if>
        <xsl:if test="$gradient/svg:stop">
            <xsl:call-template name="stops">     
                <xsl:with-param name="gradient" select="$gradient"/>
                <xsl:with-param name="type" select="$type"/>
                <xsl:with-param name="opacity" select="$opacity"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="$gradient/@x1">
            <proportional>false</proportional>
            <xsl:element name="startX">
                <xsl:call-template name="matrix-transform-x">
                    <xsl:with-param name="x" select="$gradient/@x1" />
                    <xsl:with-param name="y" select="$gradient/@y1" />
                    <xsl:with-param name="matrix-string" select="$matrix-string" />
                </xsl:call-template>  
            </xsl:element>
        </xsl:if>
        <xsl:if test="$gradient/@y1">
            <xsl:element name="startY">
                <xsl:call-template name="matrix-transform-y">
                    <xsl:with-param name="x" select="$gradient/@x1" />
                    <xsl:with-param name="y" select="$gradient/@y1" />
                    <xsl:with-param name="matrix-string" select="$matrix-string" />
                </xsl:call-template>
            </xsl:element>
        </xsl:if>
        <xsl:if test="$gradient/@x2">
            <xsl:element name="endX">  
                <xsl:call-template name="matrix-transform-x">
                    <xsl:with-param name="x" select="$gradient/@x2" />
                    <xsl:with-param name="y" select="$gradient/@y2" />
                    <xsl:with-param name="matrix-string" select="$matrix-string" />
                </xsl:call-template>
            </xsl:element>
        </xsl:if>
        <xsl:if test="$gradient/@y2">
            <xsl:element name="endY">
                <xsl:call-template name="matrix-transform-y">
                    <xsl:with-param name="x" select="$gradient/@x2" />
                    <xsl:with-param name="y" select="$gradient/@y2" />
                    <xsl:with-param name="matrix-string" select="$matrix-string" />
                </xsl:call-template>
            </xsl:element>
        </xsl:if>
        
    </xsl:template>
    
    <xsl:template name="HexToDecimal">
        <xsl:param name="hexNumber" />
        <xsl:param name="decimalNumber" >0</xsl:param>
        <xsl:choose>
            <xsl:when test="$hexNumber">
                <xsl:call-template name="HexToDecimal">
                    <xsl:with-param name="decimalNumber" select="($decimalNumber*16)+number(substring-before(substring-after('00/11/22/33/44/55/66/77/88/99/A10/B11/C12/D13/E14/F15/a10/b11/c12/d13/e14/f15/',substring($hexNumber,1,1)),'/'))" />
                    <xsl:with-param name="hexNumber" select="substring($hexNumber,2)" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$decimalNumber"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="asDecimal">
        <xsl:param name="number" />
        <xsl:choose>
            <xsl:when test="substring($number,1,2)='0x'">
                <xsl:call-template name="HexToDecimal">
                    <xsl:with-param name="hexNumber" select="substring($number,3)"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$number"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />
    
    <xsl:template name="matrix-transform-x">
        <xsl:param name="x"/>
        <xsl:param name="y"/>
        <xsl:param name="matrix-string"/>
        <xsl:variable name="coordinates" select="str:split($matrix-string,',')"/>                 
        <xsl:variable name="mxx"> 
            <xsl:value-of select="$coordinates[1]"/>
        </xsl:variable>
        <xsl:variable name="mxy"> 
            <xsl:value-of select="$coordinates[3]"/>
        </xsl:variable>
        <xsl:variable name="tx"> 
            <xsl:value-of select="$coordinates[5]"/>
        </xsl:variable>
        <xsl:value-of select="(number($mxx)*number($x))+(number($mxy)*number($y))+number($tx)"/>  
    </xsl:template>
    
    <xsl:template name="matrix-transform-y">
        <xsl:param name="x"/>
        <xsl:param name="y"/>   
        <xsl:param name="matrix-string"/>
        <xsl:variable name="coordinates" select="str:split($matrix-string,',')"/>                 
        <xsl:variable name="myx"> 
            <xsl:value-of select="$coordinates[2]"/>
        </xsl:variable>
        <xsl:variable name="myy"> 
            <xsl:value-of select="$coordinates[4]"/>
        </xsl:variable>
        <xsl:variable name="ty"> 
            <xsl:value-of select="$coordinates[6]"/>
        </xsl:variable>
        <xsl:value-of select="(number($myx)*number($x))+(number($myy)*number($y))+number($ty)"/>
    </xsl:template>  
</xsl:transform>