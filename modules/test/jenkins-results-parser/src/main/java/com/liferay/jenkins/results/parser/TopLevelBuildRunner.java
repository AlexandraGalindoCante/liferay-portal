/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.jenkins.results.parser;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import org.dom4j.Element;

/**
 * @author Michael Hashimoto
 */
public abstract class TopLevelBuildRunner
	<T extends TopLevelBuildData, S extends Workspace>
		extends BaseBuildRunner<T, S> {

	@Override
	public void run() {
		validateBuildParameters();

		publishJenkinsReport();

		updateBuildDescription();

		setUpWorkspace();

		prepareInvocationBuildDataList();

		propagateBuildDatabaseToDistNodes();

		invokeDownstreamBuilds();

		waitForDownstreamBuildsToComplete();

		publishJenkinsReport();
	}

	@Override
	public void tearDown() {
		tearDownWorkspace();

		publishJenkinsReport();
	}

	protected TopLevelBuildRunner(T topLevelBuildData) {
		super(topLevelBuildData);

		Build build = BuildFactory.newBuild(
			topLevelBuildData.getBuildURL(), null);

		if (!(build instanceof TopLevelBuild)) {
			throw new RuntimeException(
				"Invalid build URL " + topLevelBuildData.getBuildURL());
		}

		_topLevelBuild = (TopLevelBuild)build;

		topLevelBuildData.setInvocationTime(topLevelBuildData.getStartTime());
	}

	protected void addInvocationBuildData(BuildData buildData) {
		_downstreamBuildDataList.add(buildData);
	}

	protected void failBuildRunner(String message) {
		failBuildRunner(message, null);
	}

	protected void failBuildRunner(String message, Exception exception) {
		TopLevelBuildData topLevelBuildData = getBuildData();

		topLevelBuildData.setBuildDescription("<b>ERROR:</b> " + message);

		updateBuildDescription();

		if (exception == null) {
			throw new RuntimeException(message);
		}

		throw new RuntimeException(message, exception);
	}

	protected String getBuildParameter(String key) {
		TopLevelBuildData topLevelBuildData = getBuildData();

		return topLevelBuildData.getBuildParameter(key);
	}

	protected Element getJenkinsReportElement() {
		return _topLevelBuild.getJenkinsReportElement();
	}

	protected String getJobProperty(String key) {
		Job job = getJob();

		return job.getJobProperty(key);
	}

	protected TopLevelBuild getTopLevelBuild() {
		return _topLevelBuild;
	}

	protected void invokeDownstreamBuilds() {
		for (BuildData downstreamBuildData : _downstreamBuildDataList) {
			_invokeDownstreamBuild(downstreamBuildData);
		}
	}

	protected abstract void prepareInvocationBuildDataList();

	protected void propagateBuildDatabaseToDistNodes() {
		if (!JenkinsResultsParserUtil.isCINode()) {
			return;
		}

		TopLevelBuildData topLevelBuildData = getBuildData();

		File workspaceDir = topLevelBuildData.getWorkspaceDir();

		FilePropagator filePropagator = new FilePropagator(
			new String[] {BuildDatabase.BUILD_DATABASE_FILE_NAME},
			JenkinsResultsParserUtil.combine(
				topLevelBuildData.getHostname(), ":", workspaceDir.toString()),
			topLevelBuildData.getDistPath(), topLevelBuildData.getDistNodes());

		filePropagator.setCleanUpCommand(_FILE_PROPAGATOR_CLEAN_UP_COMMAND);

		filePropagator.start(_FILE_PROPAGATOR_THREAD_COUNT);
	}

	protected void publishJenkinsReport() {
		_updateBuildData();

		try {
			String jenkinsReportString = StringEscapeUtils.unescapeXml(
				Dom4JUtil.format(getJenkinsReportElement(), true));

			TopLevelBuildData topLevelBuildData = getBuildData();

			File jenkinsReportFile = new File(
				topLevelBuildData.getWorkspaceDir(), "jenkins-report.html");

			JenkinsResultsParserUtil.write(
				jenkinsReportFile, jenkinsReportString);

			publishToUserContentDir(jenkinsReportFile);
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	protected void updateJenkinsReport() {
		if (!_allBuildsAreRunning()) {
			_lastGeneratedReportTime = -1;

			return;
		}

		long currentTimeMillis = System.currentTimeMillis();

		if (_lastGeneratedReportTime == -1) {
			_lastGeneratedReportTime = System.currentTimeMillis();

			publishJenkinsReport();

			return;
		}

		if ((_lastGeneratedReportTime + _REPORT_GENERATION_INTERVAL) >
				currentTimeMillis) {

			return;
		}

		_lastGeneratedReportTime = System.currentTimeMillis();

		publishJenkinsReport();
	}

	protected abstract void validateBuildParameters();

	protected void waitForDownstreamBuildsToComplete() {
		while (true) {
			_topLevelBuild.update();

			updateJenkinsReport();

			System.out.println(_topLevelBuild.getStatusSummary());

			int completed = _topLevelBuild.getDownstreamBuildCount("completed");
			int total = _topLevelBuild.getDownstreamBuildCount(null);

			if (completed >= total) {
				break;
			}

			JenkinsResultsParserUtil.sleep(
				_WAIT_FOR_INVOKED_JOB_DURATION * 1000);
		}
	}

	private boolean _allBuildsAreRunning() {
		List<Build> runningBuilds = new ArrayList<>();

		runningBuilds.addAll(_topLevelBuild.getDownstreamBuilds("running"));
		runningBuilds.addAll(_topLevelBuild.getDownstreamBuilds("completed"));

		List<Build> totalBuilds = _topLevelBuild.getDownstreamBuilds(null);

		if (runningBuilds.size() >= totalBuilds.size()) {
			return true;
		}

		return false;
	}

	private String _getCachedJenkinsGitHubURL() {
		if (JenkinsResultsParserUtil.isCINode()) {
			Workspace workspace = getWorkspace();

			WorkspaceGitRepository jenkinsWorkspaceGitRepository =
				workspace.getJenkinsWorkspaceGitRepository();

			String gitHubDevBranchName =
				jenkinsWorkspaceGitRepository.getGitHubDevBranchName();

			if (gitHubDevBranchName != null) {
				return JenkinsResultsParserUtil.combine(
					"https://github-dev.liferay.com/liferay/",
					"liferay-jenkins-ee/tree/", gitHubDevBranchName);
			}
		}

		TopLevelBuildData topLevelBuildData = getBuildData();

		return topLevelBuildData.getJenkinsGitHubURL();
	}

	private BuildData _getDownstreamBuildData(Build downstreamBuild) {
		if (_downstreamBuildDataList.isEmpty()) {
			return null;
		}

		String buildURL = downstreamBuild.getBuildURL();

		if (buildURL == null) {
			return null;
		}

		String runID = downstreamBuild.getParameterValue("RUN_ID");

		for (BuildData downstreamBuildData : _downstreamBuildDataList) {
			if (runID.equals(downstreamBuildData.getRunID())) {
				return downstreamBuildData;
			}
		}

		return null;
	}

	private void _invokeBuild(
		String cohortName, String jobName,
		Map<String, String> invocationParameters) {

		Properties buildProperties;

		try {
			buildProperties = JenkinsResultsParserUtil.getBuildProperties();
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}

		String invocationURL =
			JenkinsResultsParserUtil.getMostAvailableMasterURL(
				JenkinsResultsParserUtil.combine(
					"http://", cohortName, ".liferay.com"),
				1);

		StringBuilder sb = new StringBuilder();

		sb.append(invocationURL);
		sb.append("/job/");
		sb.append(jobName);
		sb.append("/buildWithParameters?token=");
		sb.append(buildProperties.getProperty("jenkins.authentication.token"));

		for (Map.Entry<String, String> invocationParameter :
				invocationParameters.entrySet()) {

			sb.append("&");
			sb.append(
				JenkinsResultsParserUtil.fixURL(invocationParameter.getKey()));
			sb.append("=");
			sb.append(
				JenkinsResultsParserUtil.fixURL(
					invocationParameter.getValue()));
		}

		_topLevelBuild.addDownstreamBuilds(sb.toString());

		try {
			JenkinsResultsParserUtil.toString(sb.toString());
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	private void _invokeDownstreamBuild(BuildData buildData) {
		TopLevelBuildData topLevelBuildData = getBuildData();

		topLevelBuildData.addDownstreamBuildData(buildData);

		Map<String, String> invocationParameters = new HashMap<>();

		invocationParameters.put(
			"DIST_NODES",
			StringUtils.join(topLevelBuildData.getDistNodes(), ","));
		invocationParameters.put("DIST_PATH", topLevelBuildData.getDistPath());
		invocationParameters.put(
			"JENKINS_GITHUB_URL", _getCachedJenkinsGitHubURL());
		invocationParameters.put("RUN_ID", buildData.getRunID());
		invocationParameters.put(
			"TOP_LEVEL_RUN_ID", topLevelBuildData.getRunID());

		buildData.setInvocationTime(System.currentTimeMillis());

		_invokeBuild(
			topLevelBuildData.getCohortName(), buildData.getJobName(),
			invocationParameters);
	}

	private void _updateBuildData() {
		TopLevelBuildData topLevelBuildData = getBuildData();

		topLevelBuildData.setBuildDuration(
			System.currentTimeMillis() - topLevelBuildData.getStartTime());
		topLevelBuildData.setBuildResult(_topLevelBuild.getResult());
		topLevelBuildData.setBuildStatus(_topLevelBuild.getStatus());

		for (Build downstreamBuild : _topLevelBuild.getDownstreamBuilds(null)) {
			String buildURL = downstreamBuild.getBuildURL();

			if (buildURL == null) {
				continue;
			}

			BuildData downstreamBuildData = _getDownstreamBuildData(
				downstreamBuild);

			if (downstreamBuildData == null) {
				continue;
			}

			downstreamBuildData.setBuildDuration(downstreamBuild.getDuration());
			downstreamBuildData.setBuildResult(downstreamBuild.getResult());
			downstreamBuildData.setBuildStatus(downstreamBuild.getStatus());
			downstreamBuildData.setBuildURL(buildURL);
		}
	}

	private static final String _FILE_PROPAGATOR_CLEAN_UP_COMMAND =
		JenkinsResultsParserUtil.combine(
			"find ", BuildData.DIST_ROOT_PATH,
			"/*/* -maxdepth 1 -type d -mmin +",
			String.valueOf(TopLevelBuildRunner._FILE_PROPAGATOR_EXPIRATION),
			" -exec rm -frv {} \\;");

	private static final int _FILE_PROPAGATOR_EXPIRATION = 180;

	private static final int _FILE_PROPAGATOR_THREAD_COUNT = 1;

	private static final long _REPORT_GENERATION_INTERVAL = 1000 * 60 * 5;

	private static final int _WAIT_FOR_INVOKED_JOB_DURATION = 30;

	private final List<BuildData> _downstreamBuildDataList = new ArrayList<>();
	private long _lastGeneratedReportTime = -1;
	private final TopLevelBuild _topLevelBuild;

}