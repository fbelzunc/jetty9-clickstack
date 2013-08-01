# Jetty 9 ClickStack

Jetty 9 ClickStack for CloudBees PaaS.

# Pre-requisite

* OpenJDK 6
* Bash shellA
* Make tools
* Apache Maven

# Build 

    $ make

After successful build jetty9-plugin.zip is created and can be uploaded to the CloudBees platform location by the CloudBees team.

# Deploy

You can deploy your Jetty9 app on CloudBees using the following command:

    $ bees app:deploy -a <ACCOUNT_ID>/<APP_ID> -t jetty9 -RPLUGIN.SRC.jetty9=https://s3.amazonaws.com/cloudbees-clickstack/core/jetty9-clickstack-9b9cce5.zip app.war 


## TODOs
- [x] Support injection of Database resources
- [x] Stats
- [ ] Support injection of Mail resources
- [ ] Session stores


