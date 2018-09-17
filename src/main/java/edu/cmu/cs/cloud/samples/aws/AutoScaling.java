package edu.cmu.cs.cloud.samples.aws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
//import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.ProtocolEnum;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;
import com.amazonaws.waiters.WaiterTimedOutException;
import com.amazonaws.waiters.WaiterUnrecoverableException;

public class AutoScaling {
    private static final String KEY_NAME = "15619";
    private static final String ASG_NAME = "project2.1ASG";
    private static int minSize = 1;
    private static int maxSize = 20;
    private static int cooldown = 120;
    private static String policyNameIncrease = "increasePolicy";
    private static String policyNameDecrease = "decreasePolicy";
    private static int scalingAdjustmentIncrease = 1;
    private static int scalingAdjustmentDecrease = -1;
    private static String loadBalancerName = "project2-1LB";
    private static String launchConfigurationName = "project2-1LC";
    private static String targetGroupName = "project2-1TG2";

    public static void main(String[] args) throws IOException {
        AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();

//        CreateSecurityGroup createSecurityGroup = new CreateSecurityGroup("autoScaling");
//        String groupId = createSecurityGroup.createGroup();
        String groupId = "sg-0297732e1fcdae012";

        LaunchEC2Instance launchLG = new LaunchEC2Instance(
                "ami-013666ca8430e3646", "m3.medium", "horizontalScaling");

        Instance lgInstance;
        String lgDns = new String();
        try {
            lgInstance = launchLG.runInstance();
            lgDns = lgInstance.getPublicDnsName();
            System.out.println(lgInstance.getPublicDnsName());
            String url = "http://" + lgDns + "/password?passwd=DJ0nXHmZxJuYcAqbeAZTM2&username=wenhe@andrew.cmu.edu";
            System.out.println(url);
            buildConnection(url);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        CreateLaunchConfigurationRequest lcRequest = new CreateLaunchConfigurationRequest()
                .withLaunchConfigurationName(launchConfigurationName)
                .withImageId("ami-01b6328e1cc04b493")
                .withInstanceType("m3.medium")
                .withKeyName(KEY_NAME)
                .withInstanceMonitoring(new InstanceMonitoring().withEnabled(true))
                .withSecurityGroups("autoScaling");

        CreateTargetGroupRequest tgRequest = new CreateTargetGroupRequest()
                .withName(targetGroupName)
                .withVpcId("vpc-f2ff2f88")
                .withPort(80)
                .withProtocol(ProtocolEnum.HTTP)
                .withHealthCheckPath("/")
                .withHealthCheckProtocol(ProtocolEnum.HTTP)
                .withHealthCheckIntervalSeconds(30)
                .withHealthyThresholdCount(5)
                .withUnhealthyThresholdCount(2)
                .withHealthCheckTimeoutSeconds(10);

        // ELB launch
        AmazonElasticLoadBalancing elb = AmazonElasticLoadBalancingClientBuilder
                .standard()
                .withCredentials(credentialsProvider)
                .build();

        CreateLoadBalancerRequest lbRequest = new CreateLoadBalancerRequest()
                .withName(loadBalancerName)
                .withTags(new com.amazonaws.services.elasticloadbalancingv2.model.Tag()
                        .withKey("Project").withValue("2.1"))
                .withSecurityGroups(groupId)
                .withSubnets("subnet-f46cefda", "subnet-1dde8a57");

        String loadBalancerArn = elb.createLoadBalancer(lbRequest).getLoadBalancers()
                .get(0).getLoadBalancerArn();

        CreateTargetGroupResult targetGroupResult = elb.createTargetGroup(tgRequest);
        String targetGroupArn = targetGroupResult.getTargetGroups().get(0).getTargetGroupArn();

        CreateListenerRequest listenerRequest = new CreateListenerRequest()
                .withPort(80)
                .withProtocol("HTTP")
                .withLoadBalancerArn(loadBalancerArn)
                .withDefaultActions(new Action().withType("forward")
                        .withTargetGroupArn(targetGroupArn));

        elb.createLoadBalancer(lbRequest);
        elb.createListener(listenerRequest);

        // Auto scaling group configuration
        AmazonAutoScaling autoScaling = AmazonAutoScalingClientBuilder
                .standard()
                .withCredentials(credentialsProvider)
                .build();

        autoScaling.createLaunchConfiguration(lcRequest);

        // Auto scaling group
        CreateAutoScalingGroupRequest autoScalingGroupRequest = new CreateAutoScalingGroupRequest()
                .withAutoScalingGroupName(ASG_NAME)
                .withAvailabilityZones("us-east-1a")
//                .withLoadBalancerNames(loadBalancerName)
                .withLaunchConfigurationName(launchConfigurationName)
                .withMinSize(minSize)
                .withMaxSize(maxSize)
                .withHealthCheckGracePeriod(120)
                .withHealthCheckType("ELB")
                .withTags(new com.amazonaws.services.autoscaling.model.Tag()
                        .withKey("Project").withValue("2.1"));

        autoScaling.createAutoScalingGroup(autoScalingGroupRequest);

        PutScalingPolicyRequest scalingPolicyRequest = new PutScalingPolicyRequest()
                .withAdjustmentType("ChangeInCapacity")
                .withAutoScalingGroupName(ASG_NAME)
                .withCooldown(cooldown)
                .withPolicyName(policyNameIncrease)
                .withScalingAdjustment(scalingAdjustmentIncrease);
        String increaseArn = autoScaling.putScalingPolicy(scalingPolicyRequest).getPolicyARN();

        scalingPolicyRequest = new PutScalingPolicyRequest()
                .withAdjustmentType("ChangeInCapacity")
                .withAutoScalingGroupName(ASG_NAME)
                .withCooldown(cooldown)
                .withPolicyName(policyNameDecrease)
                .withScalingAdjustment(scalingAdjustmentDecrease);
        String decreaseArn = autoScaling.putScalingPolicy(scalingPolicyRequest).getPolicyARN();



        PutMetricAlarmRequest increaseAlarm = new PutMetricAlarmRequest()
                .withAlarmName("project2-1IA")
                .withActionsEnabled(true)
                .withAlarmActions(increaseArn)
                .withThreshold(70.0)
                .withComparisonOperator("GreaterThanThreshold")
                .withStatistic("Average")
                .withPeriod(60)
                .withEvaluationPeriods(1)
                .withMetricName("NetworkIn")
                .withNamespace("AWS/EC2");

        PutMetricAlarmRequest decreaseAlarm = new PutMetricAlarmRequest()
                .withAlarmName("project2-1DA")
                .withActionsEnabled(true)
                .withAlarmActions(decreaseArn)
                .withThreshold(70.0)
                .withComparisonOperator("LessThanThreshold")
                .withStatistic("Average")
                .withPeriod(60)
                .withEvaluationPeriods(1)
                .withMetricName("NetworkIn")
                .withNamespace("AWS/EC2");

        AmazonCloudWatch amazonCloudWatch = AmazonCloudWatchClientBuilder
                .standard()
                .withCredentials(credentialsProvider)
                .build();

        amazonCloudWatch.putMetricAlarm(increaseAlarm);
        amazonCloudWatch.putMetricAlarm(decreaseAlarm);

        Waiter waiter = elb.waiters().loadBalancerAvailable();

        try {
            waiter.run(new WaiterParameters<>(new DescribeLoadBalancersRequest()));
        } catch(WaiterUnrecoverableException e) {
            //Explicit short circuit when the resource transitions into
            //an undesired state.
       } catch(WaiterTimedOutException e) {
            //Failed to transition into desired state even after polling
       }

        Waiter waiter2 = autoScaling.waiters().groupInService();

        try {
            waiter2.run(new WaiterParameters<>(new DescribeAutoScalingGroupsRequest()));
        } catch(WaiterUnrecoverableException e) {
            //Explicit short circuit when the resource transitions into
            //an undesired state.
       } catch(WaiterTimedOutException e) {
            //Failed to transition into desired state even after polling
       }

        String elbDns = "";
        DescribeLoadBalancersRequest describeLoadBalancersRequest = new DescribeLoadBalancersRequest()
                .withNames(loadBalancerName);
        DescribeLoadBalancersResult describeLoadBalancersResult =
                elb.describeLoadBalancers(describeLoadBalancersRequest);
        elbDns = describeLoadBalancersResult.getLoadBalancers().get(0).getDNSName();

        String autoscalingUrl = "http://" + lgDns + "/autoscaling?dns=" + elbDns;
        System.out.println(autoscalingUrl);
        InputStream inputStream = buildConnection(autoscalingUrl);
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        String html = "";
        while ((inputLine = in.readLine()) != null) {
            html += inputLine;
        }
        System.out.println(html);
        in.close();
    }

    public static InputStream buildConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        int statusCode = 400;
        HttpURLConnection conn = null;
        do {
            try {
                conn = (HttpURLConnection)url.openConnection();
                statusCode = conn.getResponseCode();
            } catch (Exception e) {
                // System.err.println("Error: Connection failed!");
            }
        } while (statusCode != 200);
        return conn.getInputStream();
    }
}
