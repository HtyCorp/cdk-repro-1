import software.amazon.awscdk.core.*;
import software.amazon.awscdk.pipelines.CdkPipeline;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;

import java.util.List;

public class ReproPipelineStack extends Stack {

    public static void main(String[] args) {

        App app = new App();

        Environment defaultEnv = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();

        new ReproPipelineStack(app, "CdkPipeline", StackProps.builder().env(defaultEnv).build());

        app.synth();

    }

    public ReproPipelineStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        Artifact sourceArtifact = Artifact.artifact("repo-source");
        Artifact assemblyArtifact = Artifact.artifact("cloud-assembly");

        GitHubSourceAction gitHubSource = GitHubSourceAction.Builder.create()
                .actionName("GitHubRepoSource")
                .output(sourceArtifact)
                .oauthToken(SecretValue.secretsManager("GitHubToken"))
                .owner("HtyCorp")
                .repo("cdk-repro-1")
                .branch("master")
                .build();

        BuildEnvironment codeBuildBuildEnvironment = BuildEnvironment.builder()
                .buildImage(LinuxBuildImage.STANDARD_4_0)
                .computeType(ComputeType.MEDIUM)
                .build();
        PipelineProject codeBuildProject = PipelineProject.Builder.create(this, "CodeBuildSynth")
                .environment(codeBuildBuildEnvironment)
                .build();

        CodeBuildAction codeBuildAction = CodeBuildAction.Builder.create()
                .project(codeBuildProject)
                .actionName("BuildAndSynth")
                .input(sourceArtifact)
                .outputs(List.of(assemblyArtifact))
                .build();

        CdkPipeline pipeline = CdkPipeline.Builder.create(this, "DeploymentPipeline")
                .pipelineName("CDKDeploymentPipeline")
                .sourceAction(gitHubSource)
                .synthAction(codeBuildAction)
                .cloudAssemblyArtifact(assemblyArtifact)
                .build();

    }

}
