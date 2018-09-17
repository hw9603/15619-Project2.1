package edu.cmu.cs.cloud.samples.aws;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.IpRange;

/**
 * Class for create security group.
 */
public class CreateSecurityGroup {
    private String groupName;
    private String groupDesc = "Security group for Project 2.1";
    private String vpcId = "vpc-f2ff2f88";

    /**
     * The constructor that takes in the groupName.
     */
    public CreateSecurityGroup(String groupNameInput) {
        groupName = groupNameInput;
    }

    /**
     * Create a security group.
     */
    public String createGroup() {
        final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
        CreateSecurityGroupRequest createRequest = new CreateSecurityGroupRequest()
                .withGroupName(groupName)
                .withDescription(groupDesc)
                .withVpcId(vpcId);
        CreateSecurityGroupResult securityGroupResult = ec2.createSecurityGroup(createRequest);

        IpRange ipRange = new IpRange().withCidrIp("0.0.0.0/0");

        IpPermission ipPerm = new IpPermission()
                .withIpProtocol("tcp")
                .withToPort(80)
                .withFromPort(80)
                .withIpv4Ranges(ipRange);

        AuthorizeSecurityGroupIngressRequest authRequest = new
                AuthorizeSecurityGroupIngressRequest()
                    .withGroupName(groupName)
                    .withIpPermissions(ipPerm);

        ec2.authorizeSecurityGroupIngress(authRequest);

        return securityGroupResult.getGroupId();
    }
}
