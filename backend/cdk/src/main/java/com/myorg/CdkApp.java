package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class CdkApp {

    public static void main(final String[] args) {

        App app = new App();

        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();

        StackProps stackProps = StackProps.builder()
                .env(env)
                .build();

        VpcStack vpcStack = new VpcStack(app, "VpcStack", stackProps);

        new ComputeStack(app,
                "ComputeStack",
                vpcStack.getSagaVpc(),
                stackProps
        );

        app.synth();
    }
}

