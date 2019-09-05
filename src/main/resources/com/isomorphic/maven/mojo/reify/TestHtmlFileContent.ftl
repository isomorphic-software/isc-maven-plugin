<!doctype html>
<html>
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    
    <link type="text/css" rel="stylesheet" href="style.css">

    <title>${projectName}</title>

    <!-- IMPORTANT : You must set the variable          -->      
    <!-- isomorphicDir to [MODULE_NAME]/sc/ so that     -->
    <!-- the SmartClient resources are correctly resolved   --> 
    <script> var isomorphicDir = "./isomorphic/"; </script>  

    <script src="./isomorphic/system/modules/ISC_Core.js">          </script>
    <script src="./isomorphic/system/modules/ISC_Foundation.js">    </script>
    <script src="./isomorphic/system/modules/ISC_Containers.js">    </script>
    <script src="./isomorphic/system/modules/ISC_Grids.js">         </script>
    <script src="./isomorphic/system/modules/ISC_Forms.js">         </script>
    <script src="./isomorphic/system/modules/ISC_RichTextEditor.js"></script>
    <script src="./isomorphic/system/modules/ISC_Calendar.js">      </script>
    <script src="./isomorphic/system/modules/ISC_DataBinding.js">   </script>
    
    <script src="./isomorphic/skins/Tahoe/load_skin.js"></script>

  </head>

  <body>

    <!-- Load your project -->
    <script src="./isomorphic/projectLoader?projectName=${projectName}" type="application/javascript"></script>

    <!-- RECOMMENDED if your web app will not function without JavaScript enabled -->
    <noscript>
      <div style="width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif">
        Your web browser must have JavaScript enabled
        in order for this application to display correctly.
      </div>
    </noscript>

  </body>
</html>