Built over paw-project.sourceforge.net, this sub-project is focusing on having a proxy that instruments JavaScript,
to see how real browsers and htmlUnit process various web applications.

Requirements:
    - Java 5
    - Apache ant
    - JUnit 4 in $ANT_HOME/lib 

To run the server, type:
    ant run-server

    Which will start a local proxy listening at port 8080 (check conf/server.xml)

To shutdown the server, click the icon in the system tray (with Java 6), or close from the admin GUI.

To run the admin GUI, type:
    ant run-gui

The Admin GUI is used to stop the server, change the configurations, modify filters and handlers.

To see the logs of instrumented JavaScript, make the web browser opens the application
at the proxy hostname and port, e.g. http://localhost:8080

You can configure JavaScriptBeautifierFilter in conf/filter.xml to beautify all JavaScript files returned 
from the server side.

By default, JavaScriptFunctionLogger is used to beautify and logs entry of all functions

Roadmap:
    - Enhance the WebApp to control the instrumentation, compare real browsers and HtmlUnit behavior.

    - More instrumentation mechanisms, e.g.:
        - Log all function invocations with their parameter values e.g. [method(var1, var2)], and log all 
          assignments e.g. [var x = y + z / method2(a, b())]
        - Log all method invocations all assignments of a specific function
        - ...
