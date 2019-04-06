package org.jenkinsci.plugins;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IPMessengerSendStep extends Step {
    public String message;
    public List<String> recipientHosts;

    @DataBoundConstructor
    public IPMessengerSendStep(String message, List<String> recipientHosts) {
        this.message = message;
        this.recipientHosts = recipientHosts;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getRecipientHosts() {
        return recipientHosts;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new IPMessengerSendStepExecution(stepContext, this);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "ipmsgSend";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Notify by IPMessenger";
        }

        @Override
        public Step newInstance(@Nullable StaplerRequest req, @Nonnull JSONObject formData) throws FormException {
            String message = formData.getString("message");

            String recipientHostsInput = formData.getString("recipientHosts");
            List<String> recipientHosts = new ArrayList<>();
            for (String line : recipientHostsInput.split("\r?\n")) {
                line = line.trim();
                if (!line.isEmpty()) {
                    recipientHosts.add(line);
                }
            }

            return new IPMessengerSendStep(message, recipientHosts);
        }
    }

    private static class IPMessengerSendStepExecution extends SynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;
        private static final int SLEEP = 2500;

        private transient final IPMessengerSendStep step;

        protected IPMessengerSendStepExecution(@Nonnull StepContext context, IPMessengerSendStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            PrintStream logger = listener.getLogger();

            IPMessengerNotifier.DescriptorImpl descriptor = Jenkins.get().getDescriptorByType(IPMessengerNotifier.DescriptorImpl.class);
            String jenkinsUserName = descriptor.getJenkinsUserName() == null ? "jenkins-ci" : descriptor.getJenkinsUserName();

            Result result = getContext().get(Run.class).getResult();
            String message = "BUILD " + (result == null ? "STATUS UNKNOWN" : result.toString()) + "\n" + step.getMessage();

            String fromHost = "";
            try {
                fromHost = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                logger.println(this.getClass().getSimpleName()
                        + ": Can't get hostname of jenkins."
                        + e.getMessage());
            }

            IPMessengerSendService ipmsgService = new IPMessengerSendService(jenkinsUserName, fromHost);

            try {
                ipmsgService.sendNooperation(logger);
                Thread.sleep(SLEEP);
                for (String toHost : step.getRecipientHosts()) {
                    ipmsgService.sendMsg(message, toHost, logger);
                }
            } catch (InterruptedException e) {
                logger.println(this.getClass().getSimpleName()
                        + "IPMessengerNotifier: InterruptedException happened. "
                        + e.getMessage());
            }

            return null;
        }
    }
}
