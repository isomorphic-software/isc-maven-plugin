package com.isomorphic.maven.util;

import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.Project;

/**
 *
 *
 */
public class AntProjectLogger implements Log {

	Project project;
	
	public AntProjectLogger(Project project) {
		this.project = project;
	}
	
	@Override
	public boolean isDebugEnabled() {
		return false;
	}

	@Override
	public void debug(CharSequence content) {
		project.log(content.toString(), Project.MSG_DEBUG);
	}

	@Override
	public void debug(CharSequence content, Throwable error) {
		project.log(content.toString(), error, Project.MSG_DEBUG);
	}

	@Override
	public void debug(Throwable error) {
		project.log(null, error, Project.MSG_DEBUG);
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public void info(CharSequence content) {
		project.log("[INFO] " + content.toString(), Project.MSG_INFO);
	}

	@Override
	public void info(CharSequence content, Throwable error) {
		project.log("[INFO] " + content.toString(), error, Project.MSG_INFO);
	}

	@Override
	public void info(Throwable error) {
		project.log(null, error, Project.MSG_INFO);
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public void warn(CharSequence content) {
		project.log("[WARN] " + content.toString(), Project.MSG_WARN);
	}

	@Override
	public void warn(CharSequence content, Throwable error) {
		project.log("[WARN] " + content.toString(), error, Project.MSG_WARN);
	}

	@Override
	public void warn(Throwable error) {
		project.log(null, error, Project.MSG_WARN);
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public void error(CharSequence content) {
		project.log("[ERROR] " + content.toString(), Project.MSG_ERR);
	}

	@Override
	public void error(CharSequence content, Throwable error) {
		project.log("[ERROR] " + content.toString(), error, Project.MSG_ERR);
	}

	@Override
	public void error(Throwable error) {
		project.log(null, error, Project.MSG_ERR);
	}

	
	
}
