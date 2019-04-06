package org.jenkinsci.plugins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class IPMessengerNotifier extends Notifier {
    private static final int SLEEP = 2500;

    private String fromHost = "";
    private final String messageTemplate;
    private final String recipientHosts;

    @DataBoundConstructor
    public IPMessengerNotifier(String messageTemplate, String recipientHosts) {
        this.messageTemplate = messageTemplate;
        this.recipientHosts = recipientHosts;
    }

    public String getRecipientHosts() {
        return recipientHosts;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) {

        PrintStream logger = listener.getLogger();
        Result result = build.getResult();
        String message = "BUILD " + (result == null ? "STATUS UNKNOWN" : result.toString()) + "\n";

        try {
            fromHost = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.println(this.getClass().getSimpleName()
                    + ": Can't get hostname of jenkins."
                    + e.getMessage());
        }

        IPMessengerSendService ipmsgService = new IPMessengerSendService(getDescriptor().getJenkinsUserName(), fromHost);

        try {
            message += TokenMacro.expandAll(build, listener, messageTemplate);
            ipmsgService.sendNooperation(logger);
            Thread.sleep(SLEEP);
            for (String toHost : createRecipientHostList(recipientHosts)) {
                ipmsgService.sendMsg(message, toHost, logger);
            }
        } catch (MacroEvaluationException e) {
            logger.println(this.getClass().getSimpleName()
                    + "IPMessengerNotifier: MacroEvaluationException happened. "
                    + "Is message template correct ?"
                    + e.getMessage());
        } catch (IOException e) {
            logger.println(this.getClass().getSimpleName()
                    + "IPMessengerNotifier: IOException happened. "
            + e.getMessage());
        } catch (InterruptedException e) {
            logger.println(this.getClass().getSimpleName()
                    + "IPMessengerNotifier: InterruptedException happened. "
            + e.getMessage());
        }

        // always return true;
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Publisher> {

        private String jenkinsUserName;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Notify by IPMessenger";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws FormException {
            jenkinsUserName = formData.getString("jenkinsUserName");
            save();
            return super.configure(req, formData);
        }

        public String getJenkinsUserName() {
            return jenkinsUserName;
        }

    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    private ArrayList<String> createRecipientHostList(String recipientHosts) {
        ArrayList<String> result = new ArrayList<String>();
        for (String s : recipientHosts.split("\n")) {
            result.add(s.replaceAll("\\s+", ""));
        }
        return result;
    }
}
