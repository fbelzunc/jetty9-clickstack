plugin_name = jetty9-plugin
publish_bucket = cloudbees-clickstack
publish_repo = testing
publish_url = s3://$(publish_bucket)/$(publish_repo)/

deps = lib lib/jetty.zip lib/mysql-connector-java.jar lib/commons_collection.jar lib/commons_dbcp.jar lib/commons_pool.jar lib/jmxtrans-agent.jar java 

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

commons_collections_ver = 20040616
commons_collections_url = http://search.maven.org/remotecontent?filepath=commons-collections/commons-collections/$(commons_collections_ver)/commons-collections-$(commons_collections_ver).jar
commons_collections_md5 = a10017d52f238aebbdd0224ef15e2d8c

lib/commons_collection.jar:	
	curl -fLo lib/commons_collection.jar "$(commons_collections_url)"
	$(call check-md5,lib/commons_collection.jar,$(commons_collections_md5))


commons_dbcp_ver = 20030825.184428
commons_dbcp_url = http://search.maven.org/remotecontent?filepath=commons-dbcp/commons-dbcp/$(commons_dbcp_ver)/commons-dbcp-$(commons_dbcp_ver).jar
commons_dbcp_md5 = a0a0beadd76c8a9c7c7c039d8495caf6

lib/commons_dbcp.jar:	
	curl -fLo lib/commons_dbcp.jar "$(commons_dbcp_url)"
	$(call check-md5,lib/commons_dbcp.jar,$(commons_dbcp_md5))


commons_pool_ver = 20030825.183949
commons_pool_url = http://search.maven.org/remotecontent?filepath=commons-pool/commons-pool/$(commons_pool_ver)/commons-pool-$(commons_pool_ver).jar
commons_pool_md5 = 11125c1b5c3a86ae37e2f9ee05683d35

lib/commons_pool.jar:	
	curl -fLo lib/commons_pool.jar "$(commons_pool_url)"
	$(call check-md5,lib/commons_pool.jar,$(commons_pool_md5))


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



