<transaction xmlns:xsi="http://www.w3.org/2000/10/XMLSchema-instance" xsi:type="xsd:Object">
    <operations xsi:type="xsd:List">
        <elem xsi:type="xsd:Object">
            <appID>isc_builtin</appID>
            <className>builtin</className>
            <methodName>downloadZip</methodName>
            <arguments xsi:type="xsd:List">
                <elem xsi:type="xsd:Object">
                    <projectType>smartgwt</projectType>
                    <exportStyle>zip</exportStyle>
                    <datasourcesDir>${datasourcesDir}</datasourcesDir>
                    <includeTestData xsi:type="xsd:boolean">false</includeTestData>
                    <uiDir>${uiDir}</uiDir>
                    <includeJSP xsi:type="xsd:boolean">false</includeJSP>
                    <jspFilePath xsi:type="xsd:Object"></jspFilePath>
                    <includeHTML xsi:type="xsd:boolean">false</includeHTML>
                    <htmlFilePath xsi:type="xsd:Object"></htmlFilePath>
                    <includeProjectFile xsi:type="xsd:boolean">false</includeProjectFile>
                    <projectDir xsi:type="xsd:Object">
                        <path>${projectFileDir}/${projectFileName}</path>
                    </projectDir>
                    <#if zipFileName??>
                    <projectArchiveName>${zipFileName}</projectArchiveName>
                    <#else>
                    <projectArchiveName>${projectFileName}.proj.zip</projectArchiveName>
                    </#if>
                    <userFiles xsi:type="xsd:Object"></userFiles>
                    <screensIDs xsi:type="xsd:List">
                      <#foreach name in screens>
                        <elem>${name}</elem>
                      </#foreach>
                    </screensIDs>
                    <dataSourcesIDs xsi:type="xsd:List">
                     <#foreach name in datasources>
                        <elem>${name}</elem>
                      </#foreach>
                    </dataSourcesIDs>
                </elem>
            </arguments>
            <is_ISC_RPC_DMI xsi:type="xsd:boolean">true</is_ISC_RPC_DMI>
        </elem>
    </operations>
    <jscallback>iframe</jscallback>
</transaction>