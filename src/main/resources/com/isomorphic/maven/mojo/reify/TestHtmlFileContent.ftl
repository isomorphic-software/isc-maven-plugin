<!doctype html>
<html>
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    
    <title>${projectName}</title>

    <!-- IMPORTANT : You must set the variable          -->      
    <!-- isomorphicDir to [MODULE_NAME]/sc/ so that     -->
    <!-- the SmartClient resources are correctly resolved   --> 
    <script> var isomorphicDir = "${isomorphicDir}/"; </script>

    <script src="${isomorphicDir}/${modulesDir}/ISC_Core.js">          </script>
    <script src="${isomorphicDir}/${modulesDir}/ISC_Foundation.js">    </script>
    <script src="${isomorphicDir}/${modulesDir}/ISC_Containers.js">    </script>
    <script src="${isomorphicDir}/${modulesDir}/ISC_Grids.js">         </script>
    <script src="${isomorphicDir}/${modulesDir}/ISC_Forms.js">         </script>
    <script src="${isomorphicDir}/${modulesDir}/ISC_RichTextEditor.js"></script>
    <script src="${isomorphicDir}/${modulesDir}/ISC_Calendar.js">      </script>
    <script src="${isomorphicDir}/${modulesDir}/ISC_DataBinding.js">   </script>
    
    <script src="${isomorphicDir}/skins/Tahoe/load_skin.js"></script>

  </head>

  <body>

    <!-- Load your project -->
    <script src="${isomorphicDir}/projectLoader?projectName=${projectName}" type="application/javascript"></script>

    <!-- RECOMMENDED if your web app will not function without JavaScript enabled -->
    <noscript>
      <div style="width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif">
        Your web browser must have JavaScript enabled
        in order for this application to display correctly.
      </div>
    </noscript>

  </body>
</html>