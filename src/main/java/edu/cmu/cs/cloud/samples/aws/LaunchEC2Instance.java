package edu.cmu.cs.cloud.samples.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;

public class LaunchEC2Instance {
    private String AMI_ID;
    private String INSTANCE_TYPE;
    private String KEY_NAME       = "15619";
    private String SECURITY_GROUP = "horizontalScaling";

    LaunchEC2Instance(String _AMI_ID, String _INSTANCE_TYPE, String _SECURITY_GROUP) {
        AMI_ID = _AMI_ID;
        INSTANCE_TYPE = _INSTANCE_TYPE;
        SECURITY_GROUP = _SECURITY_GROUP;
    }

    public Instance runInstance() throws InterruptedException {
        /*
         *
         * AWS credentials provider chain that looks for credentials in this order:
         *   1. Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
         *   2. Java System Properties - aws.accessKeyId and aws.secretKey
         *   3. Instance profile credentials delivered through the Amazon EC2 metadata service
         */
        AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();

        // Create an Amazon EC2 Client
        AmazonEC2 ec2 = AmazonEC2ClientBuilder
                .standard()
                .withCredentials(credentialsProvider)
                .withRegion(Regions.US_EAST_1)
                .build();

        // Create a Run Instance Request
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
                .withImageId(AMI_ID)
                .withInstanceType(INSTANCE_TYPE)
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName(KEY_NAME)
                .withSecurityGroups(SECURITY_GROUP);

        // Execute the Run Instance Request
        RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);

        // Return the Object Reference of the Instance just Launched
        Instance instance = runInstancesResult.getReservation()
                .getInstances()
                .get(0);

        Tag tag = new Tag()
                .withKey("Project")
                .withValue("2.1");

        CreateTagsRequest createTagsRequest = new CreateTagsRequest()
                .withResources(instance.getInstanceId())
                .withTags(tag);

        ec2.createTags(createTagsRequest);

        System.out.printf("Launched instance with Instance Id: [%s]!\n", instance.getInstanceId());

        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        describeInstancesRequest.withInstanceIds(instance.getInstanceId());
        while(true) {
            long startTime = System.currentTimeMillis();
            DescribeInstancesResult describeInstancesResult =
                    ec2.describeInstances(describeInstancesRequest);
            instance = describeInstancesResult.getReservations()
                    .get(0)
                    .getInstances()
                    .get(0);
            if (instance.getState().getName().equals("running")) break;
            while (System.currentTimeMillis() - startTime < 10000) {
            }
        }
        ec2.shutdown();

        return instance;
    }
}
