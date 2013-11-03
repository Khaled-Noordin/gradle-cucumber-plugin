package com.excella.gradle.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.excella.gradle.cucumber.PatternMatcher.containsRegex;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 *
 */
public class StepDefinitions {

  private Helper helper = new Helper();
  private ProjectHelper projectHelper;
  private BuildHelper buildHelper;
  private ProcessRunner processRunner;

  @Before
  public void initHelper() throws IOException {
    helper.create();
  }

  @After
  public void closeHelper() {
    helper.delete();
  }

  @Given("^I have a new Gradle project \\(wrapper v([0-9.]+)\\) using Cucumber v([0-9.]+) for (compile|runtime)$")
  public void I_have_a_new_Gradle_project_wrapper_(String wrapperVersion, String cucumberVersion, String compileRuntime)
  throws Throwable {
    projectHelper = helper.newProjectDir();
    buildHelper = projectHelper.createBuildScript(wrapperVersion);
    buildHelper.addCucumberPlugin(cucumberVersion, !"compile".equals(compileRuntime));
  }

  @When("^I list tasks$")
  public void I_list_tasks() throws Throwable {
    buildHelper.build();

    processRunner = new ProcessRunner(buildHelper.processBuilder("tasks"));
    int exitCode = processRunner.run();

    assertEquals(0, exitCode);
  }

  @Then("^I should see a \"([^\"]*)\" task$")
  public void I_should_see_a_task(String taskName) throws Throwable {
    assertThat(processRunner.getOut(), containsRegex("^" + taskName, Pattern.MULTILINE));
  }

  @Given("^I create a \"([^\"]*)\" directory$")
  public void I_create_a_directory(String dirPath) throws Throwable {
    projectHelper.newDir(dirPath);
  }

  @Given("^I write \"([^\"]*)\" as follows$")
  public void I_write_as_follows(String path, String content) throws Throwable {
    projectHelper.newFile(path, content);
  }

  @Given("^I write a (\\w+) empty class \"([^\"]*)\"$")
  public void I_write_an_empty_class(String sourceSet, String className) throws Throwable {
    Matcher matcher = Pattern.compile("^(.*)\\.([^\\.]+)$").matcher(className);
    String packageName;
    String simpleClassName;
    if (matcher.matches()) {
      packageName = matcher.group(1);
      simpleClassName = matcher.group(2);
    } else {
      throw new RuntimeException("not a class name (or empty package name): " + className);
    }

    String path = "src/" + sourceSet + "/java/" + className.replace('.', '/') + ".java";
    projectHelper.newFile(
      path,
      "package " + packageName + ";\n" +
      "public class " + simpleClassName + " {\n" +
      "}"
    );
  }

  @Given("^I add the following task$")
  public void I_add_the_following_task(String taskCode) throws Throwable {
    buildHelper.task(taskCode);
  }

  @When("^I (successfully)? run Gradle with \"([^\"]*)\"$")
  public void I_run_Gradle_with(String successfully, String gradleArgs) throws Throwable {
    buildHelper.task("cucumber { monochrome = true }");
    buildHelper.build();

    processRunner = new ProcessRunner(buildHelper.processBuilder(gradleArgs.split(" ", -1)));
    processRunner.run();

    if (StringUtils.isNotBlank(successfully)) {
      assertThat(processRunner.getErr(), processRunner.getExitCode(), is(0));
    }
  }

  @Then("^it should succeed$")
  public void it_should_succeed() throws Throwable {
    assertThat(processRunner.getExitCode(), is(0));
  }

  @Then("^I should see a \"([^\"]*)\" line$")
  public void I_should_see_a_line(String line) throws Throwable {
    assertThat(processRunner.getExitCode(), is(0));
    assertThat(processRunner.getOut().trim(), containsRegex("^\\s*" + Pattern.quote(line) + "\\s*$", Pattern.MULTILINE));
  }

  @Then("^I should(n't| not)? see \"([^\"]*)\"$")
  public void I_shouldn_t_see(String not, String s) throws Throwable {
    if (not.isEmpty()) {
      assertThat(processRunner.getOut().trim(), containsRegex(Pattern.quote(s)));
      assertThat(processRunner.getErr().trim(), containsRegex(Pattern.quote(s)));
    } else {
      assertThat(processRunner.getOut().trim(), not(containsRegex(Pattern.quote(s))));
      assertThat(processRunner.getErr().trim(), not(containsRegex(Pattern.quote(s))));
    }
  }
}
