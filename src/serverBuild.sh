#!/bin/bash

jar vcmf ./manifests/server.mf ./Server.jar server/*.class server/*/*.class server/*/*/*.class exceptions/*.class database/*.class database/*/*.class database/*/*/*.class cloudfunctions/*.class client/rmi/ClientNotifyEventInterface.class
