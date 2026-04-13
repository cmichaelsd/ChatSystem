# ChatInfra


## Overview
This project creates a scalable API Server, Chat Server (websocket), Presence Server, and deploys a Chat Client (frontend). Please check the diagram.svg at ChatSystem root directory for a more detailed explanation of this system.


## Terraform steps
1) First time setup `terraform init`
2) After any changes are made always run `terraform fmt -recursive`
3) Validate changes `terraform validate`
4) Produce a named plan `terraform plan -out=tfplan`
5) Apply named plan to AWS `terraform apply tfplan`
6) When done run `terraform destory`


## How to access deployed application
From the terraform output will be a chatclient_url go to this endpoint and create a user.
With this user you can create a group and as a group owner you can add other users to your group.
When accessing a group messages will be real-time handled with websockets and online status indicators are near real-time, 30 second delay.