# Minecraft + Agones +  OCI

This code runs a combination of two examples
   1)  A Java MC Pinger that monitors containers running on Kubernetes cluster
   2)  A Java Wrapper bridging MC Pinger and Agones Side Car API
   3)  Minecraft world backup pushed to OCI Object Storage in GoLang.


## Install

1. Create an OKE Cluster -
    a. Public Endpoint - Public WorkerNodes
    b. Follow Create UI Workflow - Select create cluster, Select most recent version (1.20.11) , Public Nodes, Public Endpoint. c. Add kubeconfig to your existing workstation
2. Install Agones SDK.
    Agones using HELM
    ```sh
    helm repo add agones https://agones.dev/chart/stable
    helm repo update
    helm install agones --namespace agones-system --create-namespace agones/agones
    ```
    **Enable AgonesSDK Alpha Features (PlayerTracking)**
    ```sh
    helm upgrade agones agones/agones -n agones-system --reuse-values --set "agones.featureGates=PlayerTracking=true"
    ```

3. Create an AUTH Token for a user in OCI tenancy. https://docs.oracle.com/en-us/iaas/Content/devops/using/getting_started.htm#authtoken 

4. Clone agones-mc-java-oci repository in your Tenancy.

6. Create GameServers
    a. Edit file mc-server-java_agones-fleet.yml, change replicas to 1.
         Apply Server Fleet definition :  
         sed 's/<DOMAIN>/{WHATEVER DOMAIN .. ie.  example.com} /' mc-server-java_agones-fleet.yml | kubectl apply -f

7. Open Worker Node Host Port range from (7000 to 8000) to be accessible from Internet ( Seclist Ingress ) i. e 129.213.42.229
    ```sh
    $> kubectl get gameservers details
    $> kubectl describe gs
    ```
    ```sh
    NAME STATE ADDRESS PORT NODE AGE
    mc-97wjw-5bpwf Ready 129.213.42.229 7246 10.0.10.60 5m28s
    From command line (or if you prefer from OCI Console UI) create a security list for WorkerNodes on
    public subnet
    $> odax network security-list create  --ingress-security-rules  '[{"source": "0.0.0.0/0", "protocol":
    "6", "isStateless": true, "tcpOptions": {"destinationPortRange": {"max": 7000, "min": 8000},
    "sourcePortRange": {"max": 0, "min": 0}}}]' --egress-security-rules  '[{"source": "0.0.0.0/0",
    "protocol": "6", "isStateless": true, "tcpOptions": {"destinationPortRange": {"max": 0, "min": 0},
    "sourcePortRange": {"max": 0, "min": 0}}}]' --display-name "GameServerSeclist"
    a. Go to the Public Subnet - Find the associated Sectlist and add a Ingress rule that allows TCP traffic for hostPort opened by the container. In above example, Port range 7000-8000 . Source IP could me limited to specific IPs or open to all 0.0.0.0/0
    ```
8. Configure MC Backups to be stored in OCI Object Storage

    a. mc-backup contianer defined in mc-server-java*.yml files picks up the container name from ENV variable "name:BUCKET_NAME". Update value to any container of your choosing inside your tenancy.

    b. mc-backup go code uses OCI SDK to upload files and authenticates via InstancePrincipals.
        You need to create a Dynamic Group and a Policy that allows Dynamic Group to manage Object Storage.

    ```sh
    Policy Statements Example
        
        Example DynamicGroup julianDGWorkerNodesNJumphost and Compartment where bucket name is :julian
        Allow dynamic-group julianDGWorkerNodesNJumphost to read buckets in compartment julian
        Allow dynamic-group julianDGWorkerNodesNJumphost to manage objects in compartment julian where
        any {request.permission='OBJECT_CREATE', request.permission='OBJECT_INSPECT'}
        Allow dynamic-group julianDGWorkerNodesNJumphost to read buckets in compartment julian
        Allow dynamic-group julianDGWorkerNodesNJumphost to read objects in compartment julian
        Dynamic Group Matching rules can be set to either Worker node instance Id or all resources in compartment. example below
        DG - Matching Rules Example
        Any {instance.id = 'ocid1.instance.oc1.iad.anuwcljsdoggtjacafvbswta6jmah5ii54dnfrg4ZZZZZZZZZ', instance.id = 'ocid1.instance.oc1.
        iad.anuwcljtdoggtjac6bpjxbjmwsirrft4gj4ivpwbhjYYYYYYYYY', instance.id = 'ocid1.instance.
        oc1.iad.anuwcljrdoggtjacbws35ip5sffuqcx2det3scXXXXXX', resource.compartment.id =
        'ocid1.compartment.oc1..aaaaaaaaffbxtw2ofopjodaep4vtdj63kxmh5t6vtrXXXXXXXXXXXXXX'}
    ```    
    

9. Allocate Servers (Optional)
    a. kubectl create -f mc-server-java_agones-allocation.yml



## Code Base
Code is provided as is.   Also, special Thanks to :

- MCPing Java Code:  Azalim  - BR
- Agones SDK Go : saulmaldonado