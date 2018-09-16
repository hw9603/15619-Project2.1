package edu.cmu.cs.cloud.samples.aws;

import java.io.IOException;

public class AutoScaling {
    public static void main(String[] args) throws IOException {
        CreateSecurityGroup createSecurityGroup = new CreateSecurityGroup("autoScaling");
        createSecurityGroup.createGroup();

        LaunchEC2Instance launchLG = new LaunchEC2Instance("ami-013666ca8430e3646", "m3.medium", );
    }
}
