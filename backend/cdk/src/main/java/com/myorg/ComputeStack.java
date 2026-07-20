package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.ssm.IStringParameter;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

public class ComputeStack extends Stack {

    public ComputeStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ComputeStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

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


    }
}
