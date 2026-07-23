package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.ssm.IStringParameter;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ComputeStack extends Stack {

    public ComputeStack(final Construct scope, final String id, final Vpc vpc, final StackProps props) {
        super(scope, id, props);

        // creating the ecs cluster
        Cluster ecsCluster = Cluster.Builder.create(this, "SagaCluster")
                .vpc(vpc)
                .clusterName("SagaRideHailingCluster")
                .build();

        // initializing CloudMap to be attached to the cluster
        ecsCluster.addDefaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                .name("saga.local")
                .build());

        // create a task execution role so that Fargate will have permission to go to ECR, pull down the image and load it into a container
        Role taskExecutionRole = Role.Builder.create(this, "SagaTaskExecutionRole")
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .build();

        // add the policy to Agent - Pull images from ECR & push logs to CloudWatch
        taskExecutionRole.addManagedPolicy(
                ManagedPolicy.fromAwsManagedPolicyName("service-role/AmazonECSTaskExecutionRolePolicy")
        );

        // add the policy to Agent - Read the database password from SSM
        IStringParameter dbPassword = StringParameter.fromStringParameterName(
                this,
                "SagaDbPassword",
                "/saga/DATABASE_PASSWORD"
        );

        dbPassword.grantRead(taskExecutionRole);

        // add the policy to Agent - Read the database username from SSM
        IStringParameter dbUsername = StringParameter.fromStringParameterName(
                this,
                "SagaDbUsername",
                "/saga/DATABASE_USERNAME"
        );

        dbUsername.grantRead(taskExecutionRole);

        // CloudWatch log driver

        LogDriver dbLogDriver = LogDrivers.awsLogs(AwsLogDriverProps.builder()
                        .streamPrefix("SagaDatabase")
                .build());

        // db 1 - trip service

        FargateTaskDefinition tripDbTask = FargateTaskDefinition.Builder.create(this, "TripDbTask")
                .memoryLimitMiB(512)
                .cpu(256)
                .executionRole(taskExecutionRole)
                .build();

        tripDbTask.addContainer("TripDbContainer", ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry("mysql:8.4.0"))
                        .essential(true)
                        .portMappings(List.of(PortMapping.builder()
                                .containerPort(3306)
                                .build()))
                        .logging(dbLogDriver)
                        .environment(Map.of(
                                "MYSQL_DATABASE", "trip_service"
                        ))
                        .secrets(Map.of(
                                "MYSQL_ROOT_PASSWORD", Secret.fromSsmParameter(dbPassword)
                        ))
                .build());

        FargateService.Builder.create(this, "TripDbService")
                .cluster(ecsCluster)
                .taskDefinition(tripDbTask)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .cloudMapOptions(CloudMapOptions.builder()
                        .name("trip-db")
                        .build())
                .build();

        // db 2 - payment db
        FargateTaskDefinition paymentDbTask = FargateTaskDefinition.Builder.create(this, "PaymentDbTask")
                .memoryLimitMiB(512)
                .cpu(256)
                .executionRole(taskExecutionRole)
                .build();

        paymentDbTask.addContainer("PaymentDbContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("mysql:8.4.0"))
                .essential(true)
                .portMappings(List.of(PortMapping.builder()
                        .containerPort(3306)
                        .build()))
                .logging(dbLogDriver)
                .environment(Map.of(
                        "MYSQL_DATABASE", "payment_service"
                ))
                .secrets(Map.of(
                        "MYSQL_ROOT_PASSWORD", Secret.fromSsmParameter(dbPassword)
                ))
                .build());

        FargateService.Builder.create(this, "PaymentDbService")
                .cluster(ecsCluster)
                .taskDefinition(paymentDbTask)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .cloudMapOptions(CloudMapOptions.builder()
                        .name("payment-db")
                        .build())
                .build();

        // db 3 - dispatch db
        FargateTaskDefinition dispatchDbTask = FargateTaskDefinition.Builder.create(this, "DispatchDbTask")
                .memoryLimitMiB(512)
                .cpu(256)
                .executionRole(taskExecutionRole)
                .build();

        dispatchDbTask.addContainer("DispatchDbContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("mysql:8.4.0"))
                .essential(true)
                .portMappings(List.of(PortMapping.builder()
                        .containerPort(3306)
                        .build()))
                .logging(dbLogDriver)
                .environment(Map.of(
                        "MYSQL_DATABASE", "dispatch_service"
                ))
                .secrets(Map.of(
                        "MYSQL_ROOT_PASSWORD", Secret.fromSsmParameter(dbPassword)
                ))
                .build());

        FargateService.Builder.create(this, "DispatchDbService")
                .cluster(ecsCluster)
                .taskDefinition(dispatchDbTask)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .cloudMapOptions(CloudMapOptions.builder()
                        .name("dispatch-db")
                        .build())
                .build();

        // build the trip service image
        ContainerImage tripServiceImage = ContainerImage.fromAsset(
                new File("../trip-service").getAbsolutePath()
        );

        // build the payment service image
        ContainerImage paymentServiceImage = ContainerImage.fromAsset(
                new File("../payment-service").getAbsolutePath()
        );

        // build the dispatch service image
        ContainerImage dispatchServiceImage = ContainerImage.fromAsset(
                new File("../dispatch-service").getAbsolutePath()
        );

        // initialize the IAM role which the app tasks will use
        Role appTaskRole = Role.Builder.create(this, "AppTaskRole")
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .build();

        // initialize CloudWatch logs for saga apps
        LogDriver appLogDriver = LogDrivers.awsLogs(AwsLogDriverProps.builder()
                .streamPrefix("SagaApp")
                .build());

        // app 1 - trip service
        FargateTaskDefinition tripAppTask = FargateTaskDefinition.Builder.create(this, "TripAppTask")
                .memoryLimitMiB(512)
                .cpu(256)
                .executionRole(taskExecutionRole)
                .taskRole(appTaskRole)
                .build();

        tripAppTask.addContainer("TripAppContainer", ContainerDefinitionOptions.builder()
                .image(tripServiceImage)
                .essential(true)
                .portMappings(List.of(PortMapping.builder()
                        .containerPort(8080)
                        .build()))
                .logging(appLogDriver)
                .environment(Map.of(
                        "SPRING_DATASOURCE_URL", "jdbc:mysql://trip-db.saga.local:3306/trip_service?allowPublicKeyRetrieval=true&useSSL=false",
                        "SERVER_PORT", "8080"
                ))
                .secrets(Map.of(
                        "SPRING_DATASOURCE_PASSWORD", Secret.fromSsmParameter(dbPassword),
                        "SPRING_DATASOURCE_USERNAME", Secret.fromSsmParameter(dbUsername)
                ))
                .build());

        FargateService.Builder.create(this, "TripAppService")
                .cluster(ecsCluster)
                .taskDefinition(tripAppTask)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .cloudMapOptions(CloudMapOptions.builder()
                        .name("trip-service")
                        .build())
                .build();

        // app 2 - payment service
        FargateTaskDefinition paymentAppTask = FargateTaskDefinition.Builder.create(this, "PaymentAppTask")
                .memoryLimitMiB(512)
                .cpu(256)
                .executionRole(taskExecutionRole)
                .taskRole(appTaskRole)
                .build();

        paymentAppTask.addContainer("PaymentAppContainer", ContainerDefinitionOptions.builder()
                .image(paymentServiceImage)
                .essential(true)
                .portMappings(List.of(PortMapping.builder()
                        .containerPort(8080)
                        .build()))
                .logging(appLogDriver)
                .environment(Map.of(
                        "SPRING_DATASOURCE_URL", "jdbc:mysql://payment-db.saga.local:3306/payment_service?allowPublicKeyRetrieval=true&useSSL=false",
                        "SERVER_PORT", "8080"
                ))
                .secrets(Map.of(
                        "SPRING_DATASOURCE_PASSWORD", Secret.fromSsmParameter(dbPassword),
                        "SPRING_DATASOURCE_USERNAME", Secret.fromSsmParameter(dbUsername)
                ))
                .build());

        FargateService.Builder.create(this, "PaymentAppService")
                .cluster(ecsCluster)
                .taskDefinition(paymentAppTask)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .cloudMapOptions(CloudMapOptions.builder()
                        .name("payment-service")
                        .build())
                .build();

        // app 3 - dispatch service
        FargateTaskDefinition dispatchAppTask = FargateTaskDefinition.Builder.create(this, "DispatchAppTask")
                .memoryLimitMiB(512)
                .cpu(256)
                .executionRole(taskExecutionRole)
                .taskRole(appTaskRole)
                .build();

        dispatchAppTask.addContainer("DispatchAppContainer", ContainerDefinitionOptions.builder()
                .image(dispatchServiceImage)
                .essential(true)
                .portMappings(List.of(PortMapping.builder()
                        .containerPort(8080)
                        .build()))
                .logging(appLogDriver)
                .environment(Map.of(
                        "SPRING_DATASOURCE_URL", "jdbc:mysql://dispatch-db.saga.local:3306/dispatch_service?allowPublicKeyRetrieval=true&useSSL=false",
                        "SERVER_PORT", "8080"
                ))
                .secrets(Map.of(
                        "SPRING_DATASOURCE_PASSWORD", Secret.fromSsmParameter(dbPassword),
                        "SPRING_DATASOURCE_USERNAME", Secret.fromSsmParameter(dbUsername)
                ))
                .build());

        FargateService.Builder.create(this, "DispatchAppService")
                .cluster(ecsCluster)
                .taskDefinition(dispatchAppTask)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .cloudMapOptions(CloudMapOptions.builder()
                        .name("dispatch-service")
                        .build())
                .build();
    }
}
