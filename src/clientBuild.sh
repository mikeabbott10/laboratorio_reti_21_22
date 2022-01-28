#!/bin/bash

jar vcmf ./manifests/client.mf ./Client.jar client/*.class client/*/*.class exceptions/*.class server/rmi/ServerRMIInterface.class

