def call(services, body) {
    def buildingClosure = { prevFn, service, name ->
        try {
            openshiftCreateResource(getDeploymentConfigYaml(service, name))
            openshiftCreateResource(getServiceYaml(service, name))
            openshiftScale deploymentConfig: name,  replicaCount: 1, verifyReplicaCount: 1
            prevFn()
        } finally {
            openshift.withCluster() {
                try {
                    openshiftScale deploymentConfig: name,  replicaCount: 0
                } finally {
                    try {
                        openshift.selector('svc', [name: name]).delete()
                    } finally {
                        openshift.selector('dc', [name: name]).delete()
                    }
                }
            }
        }
    }

    List<String> names = getNames(services)
    def builtFunction = body;
    for (int i = 0; i < names.size(); i++) {
        builtFunction = buildingClosure.curry(builtFunction, services[i], names[i])
    }

    withEnv(env(services, names)) {
        builtFunction()
    }
}

String sanitizeObjectName(s) {
    s.replaceAll(/[_ ]/, '-').toLowerCase()
}

Map<String, String> getNames(services) {
    List<String> names = []
    for (int i = 0; i < services.size(); i++) {
        names += sanitizeObjectName("${env.BUILD_TAG}-${services[i]}")
    }
    return names
}

List<String> env(services, names) {
    List<String> out = []
    if (services.contains('mongodb')) {
        out.add("MONGODB_HOST=${names[services.indexOf('mongodb')]}")
    }
    if (services.contains('mysql')) {
        out.add("MYSQL_HOST=${names[services.indexOf('mysql')]}")
    }
    return out
}

String getDeploymentConfigYaml(service, name) {
    switch (service) {
        case 'mongodb':
        return """
apiVersion: v1
kind: DeploymentConfig
metadata:
  name: ${name}
  labels:
    name: ${name}
spec:
  replicas: 0
  selector:
    name: ${name}
  strategy:
    recreateParams:
      timeoutSeconds: 600
    type: Recreate
  template:
    metadata:
      labels:
        name: ${name}
    spec:
      containers:
      - image: docker.io/mongo:3
        imagePullPolicy: IfNotPresent
        name: mongodb
        ports:
        - containerPort: 27017
          protocol: TCP
        volumeMounts:
        - mountPath: /data/db
          name: data
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      volumes:
      - emptyDir: {}
        name: data
"""
        case 'mysql':
        return """
apiVersion: v1
kind: DeploymentConfig
metadata:
  name: ${name}
  labels:
    name: ${name}
spec:
  replicas: 0
  selector:
    name: ${name}
  strategy:
    recreateParams:
      timeoutSeconds: 600
    type: Recreate
  template:
    metadata:
      labels:
        name: ${name}
    spec:
      containers:
      - env:
        - name: MYSQL_USER
          value: mysql
        - name: MYSQL_PASSWORD
          value: password
        - name: MYSQL_ROOT_PASSWORD
          value: password
        - name: MYSQL_DATABASE
          value: sampledb
        image: registry.access.redhat.com/rhscl/mysql-57-rhel7@sha256:b8b26b5201adafb028757d7e574d377fe3e5ce5de019d783261352de2c58fcb1
        imagePullPolicy: IfNotPresent
        name: ${name}
        ports:
        - containerPort: 3306
          protocol: TCP
        volumeMounts:
        - mountPath: /var/lib/mysql/data
          name: mysql-data
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      volumes:
      - emptyDir: {}
        name: mysql-data
"""
        default:
        return ''
    }
}

String getServiceYaml(service, name) {
    switch (service) {
        case 'mongodb':
        return """
apiVersion: v1
kind: Service
metadata:
  name: ${name}
  labels:
    name: ${name}
spec:
  ports:
  - port: 27017
    protocol: TCP
    targetPort: 27017
  selector:
    name: ${name}
  type: ClusterIP
"""
        case 'mysql':
        return """
apiVersion: v1
kind: Service
metadata:
  name: ${name}
  labels:
    name: ${name}
spec:
  ports:
  - port: 3306
    protocol: TCP
    targetPort: 3306
  selector:
    name: ${name}
  type: ClusterIP
"""
        default:
        return ''
    }
}
