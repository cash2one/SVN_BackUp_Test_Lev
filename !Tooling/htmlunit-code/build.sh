ulimit -n 10000
chown -R alinebuild /opt/dfinstaller 
cp aline-devfactory-scripts/tools/build-tools.jar $M2_HOME/lib/ext
su -l alinebuild -c "java -jar /home/alinebuild/dfbuild.jar --outdir /opt/dfinstaller/dfiout --exec /opt/dfinstaller/src/build_internal.sh"