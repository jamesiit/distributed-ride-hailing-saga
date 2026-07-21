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

        // create a task execution role so that Fargate has permission to go to ECR, pull down the image and load it into a container
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
                .cloudMapOptions(CloudMapOptions.builder()
                        .name("trip-db")
                        .build())
                .build();

    }
}
