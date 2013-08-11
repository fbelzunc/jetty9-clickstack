plugin_name = jetty9-plugin
publish_bucket = cloudbees-clickstack
publish_repo = testing
publish_url = s3://$(publish_bucket)/$(publish_repo)/

deps = lib lib/jetty.zip lib/tomcat-jdbc.jar lib/tomcat-juli.jar lib/mysql-connector-java.jar  lib/jmxtrans-agent.jar java lib/cloudbees-jmx-invoker.jar

pkg_files = control functions server setup lib java conf

include plugin.mk

lib:
	mkdir -p lib

jetty_ver = 9.0.4.v20130625
jetty_url = http://eclipse.org/downloads/download.php?file=/jetty/stable-9/dist/jetty-distribution-$(jetty_ver).zip&r=1
jetty_md5 = d3d8881130f50099157411658efcd61d

lib/jetty.zip: lib lib/genapp-setup-jetty9.jar
	curl -fLo lib/jetty.zip "$(jetty_url)"
	$(call check-md5,lib/jetty.zip,$(jetty_md5))	
	unzip -qd lib lib/jetty.zip
	rm -rf lib/jetty-distribution-$(jetty_ver)/webapps.demo
	rm lib/jetty.zip
	cd lib/jetty-distribution-$(jetty_ver); \
	zip -rqy ../jetty.zip *
	rm -rf lib/jetty-distribution-$(jetty_ver)

tomcat_jdbc_ver = 7.0.42
tomcat_jdbc_url = http://repo1.maven.org/maven2/org/apache/tomcat/tomcat-jdbc/$(tomcat_jdbc_ver)/tomcat-jdbc-$(tomcat_jdbc_ver).jar
tomcat_jdbc_md5 = 0955fb87c56cb6a2790e44760dc90508

lib/tomcat-jdbc.jar:
	curl -fLo lib/tomcat-jdbc.jar "$(tomcat_jdbc_url)"
	$(call check-md5,lib/tomcat-jdbc.jar,$(tomcat_jdbc_md5))

tomcat_juli_ver = 7.0.42
tomcat_juli_url = http://repo1.maven.org/maven2/org/apache/tomcat/tomcat-juli/$(tomcat_juli_ver)/tomcat-juli-$(tomcat_juli_ver).jar
tomcat_juli_md5 = ff8d7673a10e6aca13d2ac9ab91998a1

lib/tomcat-juli.jar:
	curl -fLo lib/tomcat-juli.jar "$(tomcat_juli_url)"
	$(call check-md5,lib/tomcat-juli.jar,$(tomcat_juli_md5))

mysql_connector_ver = 5.1.25
mysql_connector_url = http://repo1.maven.org/maven2/mysql/mysql-connector-java/$(mysql_connector_ver)/mysql-connector-java-$(mysql_connector_ver).jar
mysql_connector_md5 = 46696baf8207192077ab420e5bfdc096

lib/mysql-connector-java.jar:	
	curl -fLo lib/mysql-connector-java.jar "$(mysql_connector_url)"
	$(call check-md5,lib/mysql-connector-java.jar,$(mysql_connector_md5))


jmxtrans_agent_ver = 1.0.0
jmxtrans_agent_url = http://repo1.maven.org/maven2/org/jmxtrans/agent/jmxtrans-agent/$(jmxtrans_agent_ver)/jmxtrans-agent-$(jmxtrans_agent_ver).jar
jmxtrans_agent_md5 = 9dd2bdd2adb7df9dbae093a2c6b08678

lib/jmxtrans-agent.jar: lib
	curl -fLo lib/jmxtrans-agent.jar "$(jmxtrans_agent_url)"
	$(call check-md5,lib/jmxtrans-agent.jar,$(jmxtrans_agent_md5))

jmx_invoker_ver = 1.0.2
jmx_invoker_src = http://repo1.maven.org/maven2/com/cloudbees/cloudbees-jmx-invoker/$(jmx_invoker_ver)/cloudbees-jmx-invoker-$(jmx_invoker_ver)-jar-with-dependencies.jar
jmx_invoker_md5 = c880f7545775529cfce6ea6b67277453

lib/cloudbees-jmx-invoker.jar: lib
	mkdir -p lib
	curl -fLo lib/cloudbees-jmx-invoker-jar-with-dependencies.jar "$(jmx_invoker_src)"
	$(call check-md5,lib/cloudbees-jmx-invoker-jar-with-dependencies.jar,$(jmx_invoker_md5))


lib/genapp-setup-jetty9.jar: $(JAVA_SOURCES) $(JAVA_JARS) lib
	cd genapp-setup-jetty9; \
	mvn -q clean test assembly:single; \
	cd target; \
	cp genapp-setup-jetty9-*-jar-with-dependencies.jar \
	$(CURDIR)/lib/genapp-setup-jetty9.jar


java_plugin_gitrepo = git://github.com/CloudBees-community/java-clickstack.git

java:
	git clone $(java_plugin_gitrepo) java
	rm -rf java/.git
	cd java; make clean; make deps



