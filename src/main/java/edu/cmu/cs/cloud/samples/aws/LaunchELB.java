package edu.cmu.cs.cloud.samples.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Tag;

public class LaunchELB {
    private int healthyThreshold = 10;
    private int unhealthyThreshold = 5;
    private int interval = 30;
    private String target = "HTTP:80/";
    private int timeout = 10;
    private String loadBalancerName = "project2-1LB";
    private String securityGroupId;

    public LaunchELB(String groupId) {
        securityGroupId = groupId;
    }

    public void runELB() {
        AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
        AmazonElasticLoadBalancing elb = AmazonElasticLoadBalancingClientBuilder
                .standard()
                .withCredentials(credentialsProvider)
                .build();

        Tag tag = new Tag().withKey("Project").withValue("2.1");
        CreateLoadBalancerRequest lbRequest = new CreateLoadBalancerRequest()
                .withName(loadBalancerName)
                .withTags(tag)
                .withSecurityGroups(securityGroupId)
                .withSubnets("subnet-f46cefda", "subnet-1dde8a57");

//        ConfigureHealthCheckRequest healthCheckRequest = new ConfigureHealthCheckRequest();
//        HealthCheck healthCheck = new HealthCheck()
//                .withHealthyThreshold(healthyThreshold)
//                .withUnhealthyThreshold(unhealthyThreshold)
//                .withInterval(interval)
//                .withTarget(target)
//                .withTimeout(timeout);
//        healthCheckRequest.withHealthCheck(healthCheck).withLoadBalancerName(loadBalancerName);

        CreateListenerRequest listenerRequest = new CreateListenerRequest()
                .withPort(80)
                .withProtocol("HTTP");

        elb.createLoadBalancer(lbRequest);
        elb.createListener(listenerRequest);
    }
}
