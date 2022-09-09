#Helm Charts for ingest service

##Requirements:
* Running k8s cluster
* helm 3+
* configuration service address
* zookeeper address
* rabbitmq

##Setup:
k8s cluster
minikube for single instance

```bash
# install minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
```

```bash
# start minikube
minikube delete --all --purge && \
minikube start --cpus 8 --memory 30960 --disk-size 20480 --insecure-registry="containeryard.evoforge.org" && \
minikube image load busybox:1.28
```

```bash
# update .bashrc for kubectl
alias kubectl="minikube kubectl --"
```

```bash
# test kubectl command
kubectl get pods
```

Minikube maintains its own internal registry. In order to push images to the internal minikube registry the minikube docker-env needs to be exposed
```bash
# source the minikube docker-env (this will only work for this terminal)
# all docker commands will then be forwarded to the minikube internals 
eval $(minikube -p minikube docker-env)

# build services as usual
mvn clean install -Pdocker
```

Updating values.yaml requires the following configuration. See `values.example.yaml` for an example `values.yaml`

`global.services.configuration`= configuration service address

`global.services.zookeeper` = zookeeper service address

The follow volumes must be defined

`global.volumes.pki` - pki files (secret recommended)

`global.volumes.hadoop` - hadoop config files

`global.volumes.logs` - log directory

`global.volumes.datawave` - datawave ingest configuration files

`global.volumes.output` - datawave ingest output location

`global.volumes.input` - bundler input location (suggested to match output location)

Each volume must have the following configured

`.name` - the name of the resource

`.destination` - the internal destination for the resource

`.source.type` - one of secret, configmap, hostPath

`.source.name` or `.source.path` - when using secret or configmap name must be specified to reference a valid secret or configmap. When `type` is hostPath a `source.path` must be specified

Ingest has three parts:

`feeder.feeds` - An list which defines feeds to process. Each feed must have:

`.name` - the name of the feed

`.profile` - the config service profiles to apply to this feed, comma delimited

`.queue` - the rabbit queue to push files found to

`ingest.pools` - A list of ingest processing pools. Each pool must have:

`.name` - a name to identify the pool

`.queue` - a queue to read feeder messages from (should match a queue specified in at least one feeder)

`.replicas` - the number of replicas to run for this pool

`.profiles` - the config service profiles to apply, comma delimited

`bundler` - defines what bundlers are in use, if any

`bundler.enabled` - when enabled bundlers will be deployed as a daemonset

`bundler.pools` - A list of bundlers to run in the daemonsets. Each bundler contains

`.name` - a name

`.profiles` - the config server profiles to apply to the bundler

See `values.example.yaml` for sample configuration. Any Configuration service must have the additional configs defined in `example-config`. 

###Suggested testing environment 
[minikube_helm](https://gitlab.evoforge.org/warehouses1/minikube_helm)
```bash
helm install zk zookeeper/
helm install hadoop hadoop/
helm install accumulo accumulo/

# update config service to pull in example-config in values.yaml, then
helm install web web/

helm install master ingest/
```

###Install this helm chart
```bash
helm install ingest .
```