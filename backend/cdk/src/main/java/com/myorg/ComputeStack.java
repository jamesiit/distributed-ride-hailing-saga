package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.aws_apigatewayv2_integrations.HttpAlbIntegration;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.VpcLink;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.ssm.IStringParameter;
import software.amazon.awscdk.services.ssm.SecureStringParameterAttributes;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.amazon.awscdk.services.stepfunctions.*;
import software.amazon.awscdk.services.stepfunctions.tasks.AuthType;
import software.amazon.awscdk.services.stepfunctions.tasks.CallApiGatewayHttpApiEndpoint;
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
                .containerInsightsV2(ContainerInsights.ENABLED)
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
        IStringParameter dbPassword = StringParameter.fromSecureStringParameterAttributes(this, "DBPassword",
                SecureStringParameterAttributes.builder()
                        .parameterName("/saga/DATABASE_PASSWORD")
                .build());

        dbPassword.grantRead(taskExecutionRole);

        // add the policy to Agent - Read the database username from SSM
        IStringParameter dbUsername = StringParameter.fromSecureStringParameterAttributes(this, "DBUsername",
                SecureStringParameterAttributes.builder()
                        .parameterName("/saga/DATABASE_USERNAME")
                        .build());

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

        FargateService tripDbService = FargateService.Builder.create(this, "TripDbService")
                .cluster(ecsCluster)
                .taskDefinition(tripDbTask)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .desiredCount(1)
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

        FargateService paymentDbService = FargateService.Builder.create(this, "PaymentDbService")
                .cluster(ecsCluster)
                .taskDefinition(paymentDbTask)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .desiredCount(1)
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

        FargateService dispatchDbService = FargateService.Builder.create(this, "DispatchDbService")
                .cluster(ecsCluster)
                .taskDefinition(dispatchDbTask)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .desiredCount(1)
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
                        "SERVER_PORT", "8080",
                        "SPRING_DATASOURCE_HIKARI_CONNECTIONTIMEOUT", "120000",
                        "SPRING_JPA_DATABASE_PLATFORM", "org.hibernate.dialect.MySQLDialect",
                        "SERVER_TOMCAT_KEEP_ALIVE_TIMEOUT", "70000",
                        "SERVER_TOMCAT_MAX_KEEP_ALIVE_REQUESTS", "5000"
                ))
                .secrets(Map.of(
                        "SPRING_DATASOURCE_PASSWORD", Secret.fromSsmParameter(dbPassword),
                        "SPRING_DATASOURCE_USERNAME", Secret.fromSsmParameter(dbUsername)
                ))
                .build());

        FargateService tripAppService = FargateService.Builder.create(this, "TripAppService")
                .cluster(ecsCluster)
                .taskDefinition(tripAppTask)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .desiredCount(2)
                .cloudMapOptions(CloudMapOptions.builder()
                        .name("trip-service")
                        .build())
                .healthCheckGracePeriod(Duration.seconds(180))
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
                        "SERVER_PORT", "8080",
                        "SPRING_DATASOURCE_HIKARI_CONNECTIONTIMEOUT", "120000",
                        "SPRING_JPA_DATABASE_PLATFORM", "org.hibernate.dialect.MySQLDialect",
                        "SERVER_TOMCAT_KEEP_ALIVE_TIMEOUT", "70000",
                        "SERVER_TOMCAT_MAX_KEEP_ALIVE_REQUESTS", "5000"
                ))
                .secrets(Map.of(
                        "SPRING_DATASOURCE_PASSWORD", Secret.fromSsmParameter(dbPassword),
                        "SPRING_DATASOURCE_USERNAME", Secret.fromSsmParameter(dbUsername)
                ))
                .build());

        FargateService paymentAppService = FargateService.Builder.create(this, "PaymentAppService")
                .cluster(ecsCluster)
                .taskDefinition(paymentAppTask)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .desiredCount(2)
                .cloudMapOptions(CloudMapOptions.builder()
                        .name("payment-service")
                        .build())
                .healthCheckGracePeriod(Duration.seconds(180))
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
                        "SERVER_PORT", "8080",
                        "SPRING_DATASOURCE_HIKARI_CONNECTIONTIMEOUT", "120000",
                        "SPRING_JPA_DATABASE_PLATFORM", "org.hibernate.dialect.MySQLDialect",
                        "SERVER_TOMCAT_KEEP_ALIVE_TIMEOUT", "70000",
                        "SERVER_TOMCAT_MAX_KEEP_ALIVE_REQUESTS", "5000"
                ))
                .secrets(Map.of(
                        "SPRING_DATASOURCE_PASSWORD", Secret.fromSsmParameter(dbPassword),
                        "SPRING_DATASOURCE_USERNAME", Secret.fromSsmParameter(dbUsername)
                ))
                .build());

        FargateService dispatchAppService = FargateService.Builder.create(this, "DispatchAppService")
                .cluster(ecsCluster)
                .taskDefinition(dispatchAppTask)
                .circuitBreaker(DeploymentCircuitBreaker.builder()
                        .rollback(true)
                        .build())
                .desiredCount(2)
                .cloudMapOptions(CloudMapOptions.builder()
                        .name("dispatch-service")
                        .build())
                .healthCheckGracePeriod(Duration.seconds(180))
                .build();

        // allow trip app to talk to trip db on mysql port 3306
        tripDbService.getConnections().allowFrom(tripAppService, Port.tcp(3306), "Allow Trip App Inbound");

        // allow payment app to talk to payment db on mysql port 3306
        paymentDbService.getConnections().allowFrom(paymentAppService, Port.tcp(3306), "Allow Payment App Inbound");

        // allow dispatch app to talk to dispatch db on mysql port 3306
        dispatchDbService.getConnections().allowFrom(dispatchAppService, Port.tcp(3306), "Allow Dispatch App Inbound");

        // create the public facing ALB
        ApplicationLoadBalancer alb = ApplicationLoadBalancer.Builder.create(this, "SagaAlb")
                .vpc(vpc)
                .internetFacing(false)
                .build();

        ApplicationListener listener = alb.addListener("HttpListener", BaseApplicationListenerProps.builder()
                        .port(80)
                        .defaultAction(ListenerAction.fixedResponse(404, FixedResponseOptions.builder()
                                        .contentType("text/plain")
                                        .messageBody("404 - Saga Orchestrator Route Not Found")
                                .build()))
                        .build());

        // trip target
        // trip health check
        HealthCheck tripHeathCheck = HealthCheck.builder()
                .path("/trip")
                .healthyHttpCodes("200")
                .interval(Duration.seconds(45))
                .timeout(Duration.seconds(15))
                .unhealthyThresholdCount(5)
                .build();

        listener.addTargets("TripTarget", AddApplicationTargetsProps.builder()
                .port(8080)
                .targets(List.of(tripAppService))
                .conditions(List.of(ListenerCondition.pathPatterns(List.of("/trip/*", "/trip"))))
                .priority(10)
                .healthCheck(tripHeathCheck)
                .build());

        // payment target
        // payment health check
        HealthCheck paymentHeathCheck = HealthCheck.builder()
                .path("/test/payment")
                .healthyHttpCodes("200")
                .interval(Duration.seconds(45))
                .timeout(Duration.seconds(15))
                .unhealthyThresholdCount(5)
                .build();

        listener.addTargets("PaymentTarget", AddApplicationTargetsProps.builder()
                .port(8080)
                .healthCheck(paymentHeathCheck)
                .targets(List.of(paymentAppService))
                .conditions(List.of(ListenerCondition.pathPatterns(List.of(
                        "/test/payment",
                        "/payment",
                        "/payment/*",
                        "/payments",
                        "/payments/*"
                ))))
                .priority(20)
                .build());

        // dispatch target
        // dispatch health check
        HealthCheck dispatchHealthCheck = HealthCheck.builder()
                .path("/dispatch")
                .healthyHttpCodes("200")
                .interval(Duration.seconds(45))
                .timeout(Duration.seconds(15))
                .unhealthyThresholdCount(5)
                .build();

        listener.addTargets("DispatchTarget", AddApplicationTargetsProps.builder()
                .port(8080)
                .healthCheck(dispatchHealthCheck)
                .targets(List.of(dispatchAppService))
                .conditions(List.of(ListenerCondition.pathPatterns(List.of(
                        "/dispatch",
                        "/dispatch/*"
                ))))
                .priority(30)
                .build());

        // step functions
        // api gateway http api
        HttpApi proxyApi = HttpApi.Builder.create(this, "SagaProxyApi")
                .apiName("SagaInternalProxy")
                .build();

        SecurityGroup vpcLinkSg = SecurityGroup.Builder.create(this, "SagaVpcLinkSg")
                .vpc(vpc)
                .allowAllOutbound(true)
                .description("Explicit SG for API Gateway VPC Link")
                .build();

        VpcLink vpcLink = VpcLink.Builder.create(this, "SagaVpcLink")
                .vpc(vpc)
                .vpcLinkName("saga-internal-tunnel")
                .securityGroups(List.of(vpcLinkSg))
                .build();

        HttpAlbIntegration albIntegration = HttpAlbIntegration.Builder.create("AlbIntegration", listener)
                        .vpcLink(vpcLink)
                        .build();

        alb.getConnections().allowFrom(
                vpcLinkSg,
                Port.tcp(80),
                "Allow API Gateway VPC Link to access ALB"
        );

        proxyApi.addRoutes(AddRoutesOptions.builder()
                        .path("/trip")
                        .methods(List.of(HttpMethod.POST))
                        .integration(albIntegration)
                .build());

        proxyApi.addRoutes(AddRoutesOptions.builder()
                .path("/payment")
                .methods(List.of(HttpMethod.POST))
                .integration(albIntegration)
                .build());

        proxyApi.addRoutes(AddRoutesOptions.builder()
                .path("/dispatch")
                .methods(List.of(HttpMethod.POST))
                .integration(albIntegration)
                .build());

        proxyApi.addRoutes(AddRoutesOptions.builder()
                .path("/trip/complete")
                .methods(List.of(HttpMethod.POST))
                .integration(albIntegration)
                .build());

        // step functions task states
        // trip task state
        CallApiGatewayHttpApiEndpoint createTripTask = CallApiGatewayHttpApiEndpoint.Builder.create(this, "CreateTrip")
                .apiId(proxyApi.getApiId())
                .apiStack(Stack.of(proxyApi))
                .method(software.amazon.awscdk.services.stepfunctions.tasks.HttpMethod.POST)
                .apiPath("/trip")
                .authType(AuthType.NO_AUTH)
                .requestBody(TaskInput.fromJsonPathAt("$"))
                .resultPath("$.tripResult")
                .headers(TaskInput.fromObject(Map.of(
                        "Content-Type", List.of("application/json")
                )))
                .build();

        // payment task state
        CallApiGatewayHttpApiEndpoint processPaymentTask = CallApiGatewayHttpApiEndpoint.Builder.create(this, "ProcessPayment")
                .apiId(proxyApi.getApiId())
                .apiStack(Stack.of(proxyApi))
                .method(software.amazon.awscdk.services.stepfunctions.tasks.HttpMethod.POST)
                .apiPath("/payment")
                .authType(AuthType.NO_AUTH)
                .requestBody(TaskInput.fromObject(Map.of(
                        "tripId", JsonPath.stringAt("$.tripResult.ResponseBody"),
                        "paymentAmount", 50.00
                ))).headers(TaskInput.fromObject(Map.of(
                        "Content-Type", List.of("application/json"),
                        "Idempotency-Key", JsonPath.array(JsonPath.uuid())
                )))
                .resultPath("$.paymentResult")
                .build();

        // dispatch task
        CallApiGatewayHttpApiEndpoint createDispatchTask = CallApiGatewayHttpApiEndpoint.Builder.create(this, "CreateDispatch")
                .apiId(proxyApi.getApiId())
                .apiStack(Stack.of(proxyApi))
                .method(software.amazon.awscdk.services.stepfunctions.tasks.HttpMethod.POST)
                .apiPath("/dispatch")
                .authType(AuthType.NO_AUTH)
                .requestBody(TaskInput.fromObject(Map.of(
                        "tripId", JsonPath.stringAt("$.tripResult.ResponseBody"),
                        "cabNo", "RTS-7751",
                        "cabDriver", "Armin Arlet",
                        "pickupLocation", "Wall Siena"
                )))
                .resultPath("$.dispatchResult")
                .headers(TaskInput.fromObject(Map.of(
                        "Content-Type", List.of("application/json")
                )))
                .build();

        // complete trip task
        CallApiGatewayHttpApiEndpoint completeTripTask = CallApiGatewayHttpApiEndpoint.Builder.create(this, "CompleteTrip")
                .apiId(proxyApi.getApiId())
                .apiStack(Stack.of(proxyApi))
                .method(software.amazon.awscdk.services.stepfunctions.tasks.HttpMethod.POST)
                .apiPath("/trip/complete")
                .authType(AuthType.NO_AUTH)
                .requestBody(TaskInput.fromObject(Map.of(
                        "tripId", JsonPath.stringAt("$.tripResult.ResponseBody")
                )))
                .resultPath("$.completeTrip")
                .headers(TaskInput.fromObject(Map.of(
                        "Content-Type", List.of("application/json")
                )))
                .build();

        // linking the tasks sequentially
        Chain happyPath = Chain.start(createTripTask).next(processPaymentTask).next(createDispatchTask).next(completeTripTask);

        LogGroup sagaLogGroup = LogGroup.Builder.create(this, "SagaLogGroup")
                .logGroupName("/aws/vendedlogs/states/RideHailingSagaStepFunctionsLogs")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        StateMachine sagaStateMachine = StateMachine.Builder.create(this, "SagaStateMachine")
                .stateMachineName("RideHailingSaga")
                .definitionBody(DefinitionBody.fromChainable(happyPath))
                .stateMachineType(StateMachineType.EXPRESS)
                .logs(LogOptions.builder()
                        .destination(sagaLogGroup)
                        .level(LogLevel.ALL)
                        .includeExecutionData(true)
                        .build())
                .build();

        // create the permissions for api gateway to access step functions
        Role apiGatewayRole = Role.Builder.create(this, "ApiGatewayToStepFunctionsRole")
                .assumedBy(new ServicePrincipal("apigateway.amazonaws.com"))
                .build();

        apiGatewayRole.addToPolicy(PolicyStatement.Builder.create()
                        .actions(List.of("states:StartSyncExecution"))
                        .resources(List.of(sagaStateMachine.getStateMachineArn()))
                .build());

        RestApi triggerApi = RestApi.Builder.create(this, "SagaTriggerAPI")
                .restApiName("SagaExternalTrigger")
                .build();

        AwsIntegration stepFunctionsIntegration = AwsIntegration.Builder.create()
                .service("states")
                .action("StartSyncExecution")
                .integrationHttpMethod("POST")
                .options(IntegrationOptions.builder()
                        .credentialsRole(apiGatewayRole)
                        .passthroughBehavior(PassthroughBehavior.NEVER)
                        .requestTemplates(Map.of(
                                "application/json",
                                "{ \"input\": \"$util.escapeJavaScript($input.json('$'))\", \"stateMachineArn\": \"" + sagaStateMachine.getStateMachineArn() + "\" }"
                        ))
                        .integrationResponses(List.of(
                                IntegrationResponse.builder()
                                        .statusCode("200")
                                        .responseTemplates(Map.of(
                                                "application/json",
                                                "$input.path('$')"
                                        ))
                                        .build()
                        ))
                        .build())
                .build();

        triggerApi.getRoot().addResource("start-saga")
                .addMethod("POST", stepFunctionsIntegration, MethodOptions.builder()
                        .methodResponses(List.of(MethodResponse.builder()
                                        .statusCode("200")
                                .build()))
                        .build());

        CfnOutput.Builder.create(this, "StartingUrl")
                .value(triggerApi.getUrl() + "start-saga")
                .description("The starting url")
                .build();
    }
}
