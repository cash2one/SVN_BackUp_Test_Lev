chown -R alinebuild /opt/dfinstaller 
su -l alinebuild -c "java -jar /home/alinebuild/dfbuild.jar --outdir /opt/dfinstaller/dfiout --exec /opt/dfinstaller/src/build_internal.sh"