package de.peass.ci;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import de.peass.ci.helper.GitProjectBuilder;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

public class MeasureVersionBuilderTest {
   private static final int VMS = 2;
   
   private static final int ITERATIONS = 3;
   private static final int WARMUP = 1;
   private static final int REPETITIONS = 2;
   
   @Rule
   public JenkinsRule jenkins = new JenkinsRule();
   
   @Before
   public void cleanup() throws IOException {
      final String homeFolderName = System.getenv("PEASS_HOME") != null ? System.getenv("PEASS_HOME") : System.getenv("HOME") + File.separator + ".peass" + File.separator;
      final File peassFolder = new File(homeFolderName);
      FileUtils.deleteDirectory(peassFolder);
   }

   @Test
   public void testNoGitFailure() throws Exception {
      FreeStyleProject project = jenkins.createFreeStyleProject();

      MeasureVersionBuilder builder = createSimpleBuilder();

      project.getBuildersList().add(builder);
      project = jenkins.configRoundtrip(project);

      FreeStyleBuild build = jenkins.buildAndAssertStatus(Result.FAILURE, project);
   }
   
   @Test
   public void testFullBuild() throws Exception {
      FreeStyleProject project = jenkins.createFreeStyleProject();
      initProjectFolder(project);
      
      MeasureVersionBuilder builder = createSimpleBuilder();

      project.getBuildersList().add(builder);
      project = jenkins.configRoundtrip(project);

      FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

      MeasureVersionAction action = build.getActions(MeasureVersionAction.class).get(0);

      Assert.assertEquals(ITERATIONS, action.getConfig().getIterations());
      Assert.assertEquals(VMS, action.getConfig().getVms());
      Assert.assertEquals(REPETITIONS, action.getConfig().getRepetitions());
      Assert.assertEquals(WARMUP, action.getConfig().getWarmup());
      Assert.assertEquals(0.05, action.getConfig().getType1error(), 0.01);
   }

   private void initProjectFolder(FreeStyleProject project) throws Exception, InterruptedException, IOException {
      jenkins.buildAndAssertStatus(Result.SUCCESS, project);
      
      FilePath path = project.getSomeWorkspace();
      
      File projectFolder = new File(path.toString());
      GitProjectBuilder gitbuilder = new GitProjectBuilder(projectFolder, new File("src/test/resources/peass-demo/version1"));
      gitbuilder.addVersion(new File("src/test/resources/peass-demo/version2"), "Slower Version");
   }

   private MeasureVersionBuilder createSimpleBuilder() {
      MeasureVersionBuilder builder = new MeasureVersionBuilder("test");
      builder.setIterations(ITERATIONS);
      builder.setVMs(VMS);
      builder.setRepetitions(REPETITIONS);
      builder.setWarmup(WARMUP);
      builder.setSignificanceLevel(0.05);
      builder.setExecuteRCA(false);
      return builder;
   }
}
