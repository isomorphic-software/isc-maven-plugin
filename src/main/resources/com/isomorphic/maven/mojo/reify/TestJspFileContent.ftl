<%@ taglib uri="http://www.smartclient.com/taglib" prefix="sc" %>
<%@ page language="java" pageEncoding="UTF-8"  isELIgnored="false"%>
<%
/*
 */  
%>

<!doctype html>
<html>
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    
    <title>${projectName}</title>

    <sc:loadISC skin="Tahoe" modulesDir="modules" />

  </head>

  <body>

    <!-- Load your project -->
    <script>
        <sc:loadProject name="${projectName}" />
    </script>

    <!-- RECOMMENDED if your web app will not function without JavaScript enabled -->
    <noscript>
      <div style="width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif">
        Your web browser must have JavaScript enabled
        in order for this application to display correctly.
      </div>
    </noscript>

  </body>
</html>