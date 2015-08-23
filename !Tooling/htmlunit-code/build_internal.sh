cd /opt/dfinstaller/src
rm -rf /opt/dfinstaller/src/target
rm -rf /opt/dfinstaller/dfiout
mvn clean install -P without-library-and-huge-tests $DF_MAVEN_BUILD_STATS_OPTS